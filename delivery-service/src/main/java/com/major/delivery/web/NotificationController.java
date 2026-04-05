package com.major.delivery.web;

import com.major.delivery.model.DeliveryNotification;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationFeedStore feedStore;

    public NotificationController(NotificationFeedStore feedStore) {
        this.feedStore = feedStore;
    }

    @GetMapping("/notifications/recent")
    public List<DeliveryNotification> recent() {
        return feedStore.list();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "up");
    }
}
