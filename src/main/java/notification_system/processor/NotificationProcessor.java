package notification_system.processor;

import notification_system.domain.Notification;
import notification_system.domain.NotificationStatus;
import notification_system.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationProcessor {

    private final NotificationRepository repository;

    public NotificationProcessor(NotificationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 5000)
    public void processRequestedNotifications() {

        List<Notification> notifications =
                repository.findAll()
                        .stream()
                        .filter(notification ->
                                (
                                        notification.getStatus() == NotificationStatus.REQUESTED
                                                ||
                                                (
                                                        notification.getStatus() == NotificationStatus.FAILED
                                                                && notification.getRetryCount() < 3
                                                )
                                )
                                        &&
                                        notification.isReadyToSend()
                        )
                        .toList();

        for (Notification notification : notifications) {

            try {
                notification.markAsProcessing();

                if (notification.getMessage().contains("실패")
                        && notification.getRetryCount() < 3) {
                    throw new RuntimeException("이메일 서버 오류");
                }

                notification.markAsSent();

                repository.save(notification);

                System.out.println(
                        "알림 성공: " + notification.getEventId()
                );

            } catch (Exception e) {

                notification.markAsFailed(e.getMessage());

                repository.save(notification);

                if (notification.getRetryCount() >= 3) {
                    System.out.println("최종 실패 처리 완료");
                } else {
                    System.out.println("재시도 횟수: " + notification.getRetryCount());
                }
            }
        }
    }
}