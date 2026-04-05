package com.major.delivery.web;

import com.major.delivery.model.DeliveryNotification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class NotificationFeedStore {

    private static final int MAX_ITEMS = 200;
    private final Deque<DeliveryNotification> notifications = new ConcurrentLinkedDeque<>();

    public void add(DeliveryNotification notification) {
        notifications.addFirst(notification);
        while (notifications.size() > MAX_ITEMS) {
            notifications.removeLast();
        }
    }

    public List<DeliveryNotification> list() {
        return new ArrayList<>(notifications);
    }
}
