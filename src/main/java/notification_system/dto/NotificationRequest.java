package notification_system.dto;

import notification_system.domain.NotificationChannel;

public class NotificationRequest {

    private String receiverId;
    private String message;
    private String eventId;
    private NotificationChannel channel;

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

}
