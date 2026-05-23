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


    // 테스트마다 DB 초기화
    @BeforeEach
    void clear() {
        repository.deleteAll();
    }


    // ==========================
    // 1. 알림 저장 테스트
    // ==========================

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


    // ==========================
    // 2. 중복 방지 테스트
    // ==========================

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


    // ==========================
    // 3. 읽음 처리 테스트
    // ==========================

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


    // ==========================
    // 4. 실패 처리 테스트
    // ==========================

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


    // ==========================
    // 5. 상태 변경 테스트
    // ==========================

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

}