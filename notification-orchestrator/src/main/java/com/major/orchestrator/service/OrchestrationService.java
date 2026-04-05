package com.major.orchestrator.service;

import com.major.orchestrator.model.ActionableNotification;
import com.major.orchestrator.model.DeliveryNotification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrchestrationService {

    private final KafkaTemplate<String, DeliveryNotification> kafkaTemplate;
    private final TaskScheduler taskScheduler;

    public OrchestrationService(KafkaTemplate<String, DeliveryNotification> kafkaTemplate,
                                TaskScheduler taskScheduler) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskScheduler = taskScheduler;
    }

    public void orchestrate(ActionableNotification actionable) {
        if ("SUPPRESSED".equalsIgnoreCase(actionable.getStatus())) {
            return;
        }

        DeliveryNotification deliveryNotification = toDelivery(actionable);

        if ("DELAYED".equalsIgnoreCase(actionable.getTiming()) && actionable.getDelaySeconds() > 0) {
            Instant when = Instant.now().plusSeconds(actionable.getDelaySeconds());
            taskScheduler.schedule(() -> kafkaTemplate.send("delivery-stream", actionable.getUserId(), deliveryNotification), when);
            return;
        }

        kafkaTemplate.send("delivery-stream", actionable.getUserId(), deliveryNotification);
    }

    private DeliveryNotification toDelivery(ActionableNotification actionable) {
        DeliveryNotification notification = new DeliveryNotification();
        notification.setNotificationId(actionable.getNotificationId());
        notification.setUserId(actionable.getUserId());
        notification.setPriority(actionable.getPriority());
        notification.setChannel(resolveChannel(actionable));
        notification.setMessage(actionable.getMessage());
        notification.setSource(actionable.getEventType());
        notification.setDispatchedAt(System.currentTimeMillis());
        notification.setMetadata(actionable.getMetadata());
        notification.getMetadata().put("timing", actionable.getTiming());
        notification.getMetadata().put("score", actionable.getScore());
        return notification;
    }

    private String resolveChannel(ActionableNotification actionable) {
        if ("CRITICAL".equalsIgnoreCase(actionable.getPriority())) {
            return "SMS";
        }
        if ("BATTERY_LOW".equalsIgnoreCase(actionable.getEventType())) {
            return "PUSH";
        }
        return "PUSH";
    }
}
