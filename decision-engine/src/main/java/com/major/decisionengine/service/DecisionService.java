package com.major.decisionengine.service;

import com.major.decisionengine.model.ActionableNotification;
import com.major.decisionengine.model.EnrichedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class DecisionService {

    private static final Duration MESSAGE_WINDOW_TTL = Duration.ofSeconds(45);
    private static final Duration LAST_SENT_TTL = Duration.ofMinutes(10);
    private static final long RATE_LIMIT_WINDOW_MILLIS = 20_000;

    private final StringRedisTemplate redisTemplate;

    public DecisionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ActionableNotification evaluate(EnrichedEvent event) {
        ActionableNotification result = base(event);

        if ("CRITICAL".equalsIgnoreCase(event.getPriority()) || "CRITICAL_ALERT".equalsIgnoreCase(event.getType())) {
            result.setTiming("IMMEDIATE");
            result.setStatus("READY");
            result.setChannel("PUSH");
            updateLastSent(event.getUserId());
            return result;
        }

        boolean isActive = Boolean.parseBoolean(String.valueOf(event.getContext().getOrDefault("isActive", "true")));
        if (!isActive && "LOW".equalsIgnoreCase(event.getPriority())) {
            result.setStatus("SUPPRESSED");
            result.setTiming("NONE");
            result.setMessage("Suppressed low-priority event while user is inactive");
            result.setChannel("NONE");
            return result;
        }

        if ("MESSAGE".equalsIgnoreCase(event.getType())) {
            ActionableNotification mergedDecision = evaluateMessageMerge(event, result);
            if ("READY".equalsIgnoreCase(mergedDecision.getStatus())) {
                applyRateLimitIfNeeded(mergedDecision, event.getUserId());
            }
            if (!"SUPPRESSED".equalsIgnoreCase(mergedDecision.getStatus())) {
                updateLastSent(event.getUserId());
            }
            return mergedDecision;
        }

        result.setStatus("READY");
        result.setTiming("IMMEDIATE");
        result.setChannel("PUSH");
        applyRateLimitIfNeeded(result, event.getUserId());
        updateLastSent(event.getUserId());
        return result;
    }

    private ActionableNotification evaluateMessageMerge(EnrichedEvent event, ActionableNotification base) {
        String key = "user:message-window:" + event.getUserId();
        long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, MESSAGE_WINDOW_TTL);

        if (count % 5 != 0) {
            base.setStatus("SUPPRESSED");
            base.setTiming("NONE");
            base.setChannel("NONE");
            base.setMessage("Buffered low-priority message for merge: " + count + "/5");
            return base;
        }

        base.setStatus("READY");
        base.setTiming("DELAYED");
        base.setDelaySeconds(15);
        base.setChannel("PUSH");
        base.setMessage("You have 5 new messages");
        base.getMetadata().put("mergedCount", 5);
        return base;
    }

    private void applyRateLimitIfNeeded(ActionableNotification notification, String userId) {
        String key = "user:last-sent:" + userId;
        String value = redisTemplate.opsForValue().get(key);
        long now = System.currentTimeMillis();

        if (value != null) {
            long last = parseLong(value);
            if (now - last < RATE_LIMIT_WINDOW_MILLIS && !"CRITICAL".equalsIgnoreCase(notification.getPriority())) {
                notification.setTiming("DELAYED");
                notification.setDelaySeconds(Math.max(notification.getDelaySeconds(), 20));
                notification.setMessage("Rate-limited: " + notification.getMessage());
            }
        }
    }

    private void updateLastSent(String userId) {
        redisTemplate.opsForValue().set("user:last-sent:" + userId, String.valueOf(System.currentTimeMillis()), LAST_SENT_TTL);
    }

    private ActionableNotification base(EnrichedEvent event) {
        ActionableNotification notification = new ActionableNotification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(event.getUserId());
        notification.setSourceEventId(event.getEventId());
        notification.setEventType(event.getType());
        notification.setPriority(event.getPriority());
        notification.setMessage(event.getMessage());
        notification.setScore(event.getImportanceScore());
        notification.setCreatedAt(System.currentTimeMillis());
        notification.getMetadata().putAll(event.getContext());
        return notification;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
