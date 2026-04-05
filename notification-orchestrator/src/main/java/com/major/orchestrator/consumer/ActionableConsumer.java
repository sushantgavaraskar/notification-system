package com.major.orchestrator.consumer;

import com.major.orchestrator.model.ActionableNotification;
import com.major.orchestrator.service.OrchestrationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ActionableConsumer {

    private final OrchestrationService orchestrationService;

    public ActionableConsumer(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(topics = "actionable-notifications", groupId = "orchestrator-group")
    public void consume(ActionableNotification notification) {
        orchestrationService.orchestrate(notification);
    }
}
