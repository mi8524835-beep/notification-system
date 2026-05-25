package notification_system.repository;

import notification_system.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import notification_system.domain.NotificationStatus;

import notification_system.domain.NotificationChannel;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification>
    findByReceiverIdAndEventIdAndChannel(

            String receiverId,

            String eventId,

            NotificationChannel channel

    );

    List<Notification> findByReceiverId(String receiverId);

    List<Notification> findByReceiverIdAndReadStatus(
            String receiverId,
            Boolean readStatus
    );

    List<Notification>
    findByStatus(
            NotificationStatus status
    );

    long countByStatus(NotificationStatus status);

    long countByReceiverIdAndStatus(
            String receiverId,
            NotificationStatus status
    );

}