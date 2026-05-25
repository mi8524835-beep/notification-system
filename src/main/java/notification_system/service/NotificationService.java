package notification_system.service;

import notification_system.domain.Notification;
import notification_system.domain.NotificationStatus;
import notification_system.repository.NotificationRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void send(Notification notification) {

        long failedCount =
                repository.countByReceiverIdAndStatus(
                        notification.getReceiverId(),
                        NotificationStatus.FAILED
                );

        if (failedCount >= 10) {

            System.out.println(
                    "반복 실패 사용자 - 알림 요청 차단"
            );

            return;
        }

        if (repository.findByReceiverIdAndEventId(
                notification.getReceiverId(),
                notification.getEventId()
        ).isPresent()) {

            System.out.println(
                    "중복 알림 요청 - 저장하지 않음"
            );

            return;
        }

        try {

            repository.save(
                    notification
            );

            System.out.println(
                    "알림 요청 접수"
            );

            System.out.println(
                    "REQUESTED 저장 완료"
            );

        } catch (DataIntegrityViolationException e) {

            System.out.println(
                    "동시 중복 알림 요청 - 저장하지 않음"
            );
        }
    }
}