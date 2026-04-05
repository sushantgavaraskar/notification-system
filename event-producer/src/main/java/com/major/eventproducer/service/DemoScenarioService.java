package com.major.eventproducer.service;

import com.major.eventproducer.model.UserEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DemoScenarioService {

    public List<UserEvent> buildScenario(String userId, String scenario) {
        return switch (scenario.toLowerCase()) {
            case "message-burst" -> messageBurst(userId);
            case "battery-low" -> List.of(singleEvent(userId, "BATTERY_LOW", "MEDIUM", "Battery dropped below 15%"));
            case "user-inactive" -> List.of(userInactiveEvent(userId));
            case "critical-alert" -> List.of(singleEvent(userId, "CRITICAL_ALERT", "CRITICAL", "Suspicious login detected"));
            case "full-story" -> fullStory(userId);
            default -> List.of(singleEvent(userId, "INFO", "LOW", "Unknown scenario fallback event"));
        };
    }

    private List<UserEvent> messageBurst(String userId) {
        List<UserEvent> events = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            events.add(singleEvent(userId, "MESSAGE", "LOW", "You have a new message (" + i + "/5)"));
        }
        return events;
    }

    private UserEvent userInactiveEvent(String userId) {
        UserEvent event = singleEvent(userId, "USER_ACTIVITY", "LOW", "User has been inactive for 30 minutes");
        event.getMetadata().put("active", false);
        event.getMetadata().put("inactiveMinutes", 30);
        return event;
    }

    private List<UserEvent> fullStory(String userId) {
        List<UserEvent> events = new ArrayList<>();
        events.addAll(messageBurst(userId));
        events.add(singleEvent(userId, "BATTERY_LOW", "MEDIUM", "Battery dropped below 10%"));
        events.add(userInactiveEvent(userId));
        events.add(singleEvent(userId, "CRITICAL_ALERT", "CRITICAL", "Critical security alert detected"));
        return events;
    }

    private UserEvent singleEvent(String userId, String type, String priority, String message) {
        UserEvent event = new UserEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setType(type);
        event.setPriority(priority);
        event.setMessage(message);
        event.setCreatedAt(System.currentTimeMillis());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "demo-simulator");
        event.setMetadata(metadata);
        return event;
    }
}
