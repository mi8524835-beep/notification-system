package notification_system.service;

import notification_system.domain.Notification;
import notification_system.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void send(Notification notification) {

        if (repository.findByReceiverIdAndEventId(
                notification.getReceiverId(),
                notification.getEventId()
        ).isPresent()) {

            System.out.println("중복 알림 요청 - 저장하지 않음");
            return;
        }

        repository.save(notification);   // ⭐ 저장

        System.out.println("알림 요청 접수");
        System.out.println("REQUESTED 저장 완료");
    }
}