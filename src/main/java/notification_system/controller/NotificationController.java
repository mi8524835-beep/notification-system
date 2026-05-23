package notification_system.controller;

import notification_system.domain.Notification;
import notification_system.domain.NotificationChannel;
import notification_system.domain.NotificationStatus;
import notification_system.dto.NotificationRequest;
import notification_system.repository.NotificationRepository;
import notification_system.service.NotificationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService service;

    public NotificationController(
            NotificationRepository notificationRepository,
            NotificationService service
    ) {
        this.notificationRepository = notificationRepository;
        this.service = service;
    }

    @PostMapping("/notifications")
    public String createNotification(
            @RequestBody NotificationRequest request
    ) {
        Notification notification =
                new Notification(
                        request.getReceiverId(),
                        request.getMessage(),
                        request.getEventId(),
                        NotificationStatus.REQUESTED,
                        request.getChannel()
                );

        service.send(notification);

        return "알림 요청 접수 완료";
    }

    @GetMapping("/notifications")
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @GetMapping("/notifications/{id}")
    public Notification getNotification(
            @PathVariable Long id
    ) {
        return notificationRepository.findById(id)
                .orElseThrow();
    }

    @PatchMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable Long id
    ) {
        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow();

        notification.markAsRead();

        notificationRepository.save(notification);

        return "읽음 처리 완료";
    }

    @GetMapping("/users/{receiverId}/notifications")
    public List<Notification> getUserNotifications(
            @PathVariable String receiverId,
            @RequestParam(required = false) Boolean readStatus
    ) {
        if (readStatus == null) {
            return notificationRepository.findByReceiverId(receiverId);
        }

        return notificationRepository.findByReceiverIdAndReadStatus(
                receiverId,
                readStatus
        );
    }
}