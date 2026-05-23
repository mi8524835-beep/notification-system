package notification_system.dto;

import notification_system.domain.NotificationChannel;

import java.time.LocalDateTime;

public class NotificationRequest {

    private String receiverId;
    private String message;
    private String eventId;
    private NotificationChannel channel;

    private LocalDateTime scheduledAt;

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }
}