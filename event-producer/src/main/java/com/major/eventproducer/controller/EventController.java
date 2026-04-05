package com.major.eventproducer.controller;

import com.major.eventproducer.model.UserEvent;
import com.major.eventproducer.service.DemoScenarioService;
import com.major.eventproducer.service.EventPublisherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventPublisherService publisherService;
    private final DemoScenarioService demoScenarioService;

    public EventController(EventPublisherService publisherService, DemoScenarioService demoScenarioService) {
        this.publisherService = publisherService;
        this.demoScenarioService = demoScenarioService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> publishEvent(@Valid @RequestBody UserEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getCreatedAt() == 0L) {
            event.setCreatedAt(System.currentTimeMillis());
        }
        publisherService.publish(event);
        return ResponseEntity.ok(Map.of(
                "status", "published",
                "eventId", event.getEventId(),
                "userId", event.getUserId()
        ));
    }

    @PostMapping("/demo/{scenario}")
    public ResponseEntity<Map<String, Object>> runDemoScenario(
            @PathVariable String scenario,
            @RequestParam(defaultValue = "user-1") String userId
    ) {
        List<UserEvent> events = demoScenarioService.buildScenario(userId, scenario);
        events.forEach(publisherService::publish);
        return ResponseEntity.ok(Map.of(
                "status", "published",
                "scenario", scenario,
                "count", events.size(),
                "userId", userId
        ));
    }

    @GetMapping("/demo/scenarios")
    public List<String> listScenarios() {
        return List.of("message-burst", "battery-low", "user-inactive", "critical-alert", "full-story");
    }
}
