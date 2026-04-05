package com.major.contextengine.service;

import com.major.contextengine.model.EnrichedEvent;
import com.major.contextengine.model.UserEvent;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContextStateService {

    private static final Duration CONTEXT_TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public ContextStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public EnrichedEvent enrich(UserEvent event) {
        String key = contextKey(event.getUserId());
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();

        updateContextFromEvent(hashOps, key, event);
        redisTemplate.expire(key, CONTEXT_TTL);

        Map<String, Object> context = snapshot(hashOps.entries(key));

        EnrichedEvent enriched = new EnrichedEvent();
        enriched.setEventId(event.getEventId());
        enriched.setUserId(event.getUserId());
        enriched.setType(event.getType());
        enriched.setPriority(event.getPriority());
        enriched.setMessage(event.getMessage());
        enriched.setCreatedAt(event.getCreatedAt());
        enriched.setMetadata(event.getMetadata());
        enriched.setContext(context);
        enriched.setImportanceScore(score(event, context));
        return enriched;
    }

    private void updateContextFromEvent(HashOperations<String, Object, Object> hashOps, String key, UserEvent event) {
        long now = System.currentTimeMillis();
        hashOps.put(key, "lastEventType", event.getType());
        hashOps.put(key, "lastPriority", event.getPriority());
        hashOps.put(key, "lastUpdated", String.valueOf(now));

        if ("USER_ACTIVITY".equalsIgnoreCase(event.getType())) {
            Object active = event.getMetadata().getOrDefault("active", Boolean.TRUE);
            hashOps.put(key, "isActive", String.valueOf(active));
            if (Boolean.TRUE.toString().equalsIgnoreCase(String.valueOf(active))) {
                hashOps.put(key, "lastActiveAt", String.valueOf(now));
            }
        }

        if ("MESSAGE".equalsIgnoreCase(event.getType())) {
            long count = readLong(hashOps.get(key, "pendingMessageCount"));
            hashOps.put(key, "pendingMessageCount", String.valueOf(count + 1));
        }

        if ("BATTERY_LOW".equalsIgnoreCase(event.getType())) {
            hashOps.put(key, "batteryLow", "true");
        }

        if ("CRITICAL_ALERT".equalsIgnoreCase(event.getType())) {
            hashOps.put(key, "hasCriticalSignal", "true");
        }
    }

    private int score(UserEvent event, Map<String, Object> context) {
        int score = switch (event.getPriority().toUpperCase()) {
            case "CRITICAL" -> 100;
            case "HIGH" -> 75;
            case "MEDIUM" -> 50;
            default -> 20;
        };

        if (Boolean.parseBoolean(String.valueOf(context.getOrDefault("isActive", "true")))) {
            score += 5;
        } else {
            score -= 10;
        }

        if ("MESSAGE".equalsIgnoreCase(event.getType())) {
            long pending = parseLong(context.getOrDefault("pendingMessageCount", "0"));
            if (pending >= 3) {
                score -= 5;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    private Map<String, Object> snapshot(Map<Object, Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        result.putIfAbsent("isActive", "true");
        result.putIfAbsent("pendingMessageCount", "0");
        return result;
    }

    private String contextKey(String userId) {
        return "user:context:" + userId;
    }

    private long readLong(Object value) {
        if (value == null) {
            return 0;
        }
        return parseLong(value);
    }

    private long parseLong(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
