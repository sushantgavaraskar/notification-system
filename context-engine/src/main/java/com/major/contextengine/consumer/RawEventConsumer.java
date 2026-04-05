package com.major.contextengine.consumer;

import com.major.contextengine.model.EnrichedEvent;
import com.major.contextengine.model.UserEvent;
import com.major.contextengine.service.ContextStateService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RawEventConsumer {

    private final ContextStateService contextStateService;
    private final KafkaTemplate<String, EnrichedEvent> kafkaTemplate;

    public RawEventConsumer(ContextStateService contextStateService,
                            KafkaTemplate<String, EnrichedEvent> kafkaTemplate) {
        this.contextStateService = contextStateService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "raw-events", groupId = "context-engine-group")
    public void consume(UserEvent event) {
        EnrichedEvent enrichedEvent = contextStateService.enrich(event);
        kafkaTemplate.send("enriched-events", event.getUserId(), enrichedEvent);
    }
}
