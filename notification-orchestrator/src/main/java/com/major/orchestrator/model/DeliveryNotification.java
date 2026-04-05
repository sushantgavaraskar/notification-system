package com.major.orchestrator.model;

import java.util.HashMap;
import java.util.Map;

public class DeliveryNotification {

    private String notificationId;
    private String userId;
    private String priority;
    private String channel;
    private String message;
    private String source;
    private long dispatchedAt;
    private Map<String, Object> metadata = new HashMap<>();

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(long dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
