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

import jakarta.persistence.Version;

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

    @Version
    private Long version;

    private String receiverId;

    private String message;

    private String eventId;

    private Integer retryCount = 0;

    private Boolean readStatus = false;

    private String failureReason;

    private LocalDateTime scheduledAt;

    private LocalDateTime processingStartedAt;

    private LocalDateTime nextRetryAt;

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

    public Long getVersion() {
        return version;
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

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public boolean isReadyToSend() {
        return scheduledAt == null
                || scheduledAt.isBefore(LocalDateTime.now())
                || scheduledAt.isEqual(LocalDateTime.now());
    }

    public boolean isReadyToRetry() {
        return nextRetryAt == null
                || nextRetryAt.isBefore(LocalDateTime.now())
                || nextRetryAt.isEqual(LocalDateTime.now());
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.failureReason = null;
        this.nextRetryAt = null;
    }

    public void markAsProcessing() {

        this.status =
                NotificationStatus.PROCESSING;

        this.processingStartedAt =
                LocalDateTime.now();
    }

    public void forceProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public void markAsRead() {
        if (Boolean.TRUE.equals(this.readStatus)) {
            return;
        }

        this.readStatus = true;
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.retryCount++;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(getRetryDelaySeconds());
    }

    public void retry() {
        this.status = NotificationStatus.REQUESTED;
        this.retryCount = 0;
        this.failureReason = null;
        this.nextRetryAt = null;
    }

    public boolean isProcessingTooLong() {

        return status ==
                NotificationStatus.PROCESSING

                &&

                processingStartedAt != null

                &&

                processingStartedAt.isBefore(
                        LocalDateTime.now()
                                .minusMinutes(30)
                );
    }

    private long getRetryDelaySeconds() {
        if (retryCount == 1) {
            return 5;
        }

        if (retryCount == 2) {
            return 30;
        }

        return 300;
    }
}