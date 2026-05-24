package notification_system;

import notification_system.domain.*;
import notification_system.repository.NotificationRepository;
import notification_system.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    NotificationService service;

    @Autowired
    NotificationRepository repository;


    @BeforeEach
    void clear() {
        repository.deleteAll();
    }


    @Test
    void 알림저장테스트() {

        Notification notification =
                new Notification(
                        "민경",
                        "테스트",
                        "EVENT_001",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        service.send(notification);

        Notification saved =
                repository.findAll().get(0);

        assertThat(
                saved.getReceiverId()
        ).isEqualTo("민경");
    }


    @Test
    void 중복알림은저장하지않는다() {

        Notification first =
                new Notification(
                        "민경",
                        "중복",
                        "EVENT_DUP",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        Notification second =
                new Notification(
                        "민경",
                        "중복",
                        "EVENT_DUP",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        service.send(first);
        service.send(second);

        assertThat(
                repository.findAll().size()
        ).isEqualTo(1);
    }


    @Test
    void 읽음처리테스트() {

        Notification notification =
                new Notification(
                        "민경",
                        "읽음",
                        "EVENT_READ",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsRead();

        assertThat(
                notification.getReadStatus()
        ).isTrue();
    }


    @Test
    void 실패시재시도횟수증가() {

        Notification notification =
                new Notification(
                        "민경",
                        "실패",
                        "EVENT_FAIL",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsFailed(
                "서버 오류"
        );

        assertThat(
                notification.getRetryCount()
        ).isEqualTo(1);

        assertThat(
                notification.getStatus()
        ).isEqualTo(
                NotificationStatus.FAILED
        );
    }


    @Test
    void 상태변경테스트() {

        Notification notification =
                new Notification(
                        "민경",
                        "성공",
                        "EVENT_SUCCESS",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsSent();

        assertThat(
                notification.getStatus()
        ).isEqualTo(
                NotificationStatus.SENT
        );
    }


    @Test
    void 템플릿메시지테스트() {

        assertThat(
                NotificationTemplate.PAYMENT_SUCCESS.getMessage()
        ).isEqualTo("결제가 완료되었습니다.");
    }


    @Test
    void 실패시다음재시도시간이설정된다() {

        Notification notification =
                new Notification(
                        "민경",
                        "실패",
                        "EVENT_FAIL_RETRY",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsFailed("서버 오류");

        assertThat(
                notification.getNextRetryAt()
        ).isNotNull();
    }
}