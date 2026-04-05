package com.major.delivery.consumer;

import com.major.delivery.model.DeliveryNotification;
import com.major.delivery.web.NotificationFeedStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationFeedStore feedStore;

    public DeliveryConsumer(SimpMessagingTemplate messagingTemplate, NotificationFeedStore feedStore) {
        this.messagingTemplate = messagingTemplate;
        this.feedStore = feedStore;
    }

    @KafkaListener(topics = "delivery-stream", groupId = "delivery-service-group")
    public void consume(DeliveryNotification notification) {
        feedStore.add(notification);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
