package notification_system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"receiverId", "eventId"}
                )
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiverId;

    private String message;

    private String eventId;

    private Integer retryCount = 0;

    private Boolean readStatus = false;

    private String failureReason;

    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    protected Notification() {
    }

    public Notification(
            String receiverId,
            String message,
            String eventId,
            NotificationStatus status,
            NotificationChannel channel
    ) {
        this.receiverId = receiverId;
        this.message = message;
        this.eventId = eventId;
        this.status = status;
        this.channel = channel;
        this.scheduledAt = LocalDateTime.now();
    }

    public Notification(
            String receiverId,
            String message,
            String eventId,
            NotificationStatus status,
            NotificationChannel channel,
            LocalDateTime scheduledAt
    ) {
        this.receiverId = receiverId;
        this.message = message;
        this.eventId = eventId;
        this.status = status;
        this.channel = channel;
        this.scheduledAt = scheduledAt;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public Long getId() {
        return id;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public boolean isReadyToSend() {
        return scheduledAt == null
                || scheduledAt.isBefore(LocalDateTime.now())
                || scheduledAt.isEqual(LocalDateTime.now());
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markAsProcessing() {
        this.status = NotificationStatus.PROCESSING;
    }

    public void markAsRead() {
        this.readStatus = true;
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.retryCount++;
    }

    public void retry() {
        this.status = NotificationStatus.REQUESTED;
        this.retryCount = 0;
        this.failureReason = null;
    }
}