package com.major.decisionengine.consumer;

import com.major.decisionengine.model.ActionableNotification;
import com.major.decisionengine.model.EnrichedEvent;
import com.major.decisionengine.service.DecisionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnrichedEventConsumer {

    private final DecisionService decisionService;
    private final KafkaTemplate<String, ActionableNotification> kafkaTemplate;

    public EnrichedEventConsumer(DecisionService decisionService,
                                 KafkaTemplate<String, ActionableNotification> kafkaTemplate) {
        this.decisionService = decisionService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "enriched-events", groupId = "decision-engine-group")
    public void consume(EnrichedEvent event) {
        ActionableNotification decision = decisionService.evaluate(event);
        kafkaTemplate.send("actionable-notifications", event.getUserId(), decision);
    }
}
