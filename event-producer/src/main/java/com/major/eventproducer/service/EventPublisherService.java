package com.major.eventproducer.service;

import com.major.eventproducer.model.UserEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public EventPublisherService(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UserEvent event) {
        kafkaTemplate.send("raw-events", event.getUserId(), event);
    }
}
