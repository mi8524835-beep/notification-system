package notification_system;

import notification_system.domain.*;
import notification_system.repository.NotificationRepository;
import notification_system.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Assertions;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    NotificationService service;

    @Autowired
    NotificationRepository repository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

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

    @Test
    void 이미읽음상태면다시읽음처리해도상태는유지된다() {

        Notification notification =
                new Notification(
                        "민경",
                        "읽음",
                        "EVENT_READ_ONCE",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsRead();
        notification.markAsRead();

        assertThat(
                notification.getReadStatus()
        ).isTrue();
    }

    @Test
    void 동시에같은알림을수정하면낙관적락이동작한다() {

        Notification notification =
                new Notification(
                        "민경",
                        "읽음",
                        "EVENT_CONCURRENT_READ",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        repository.save(notification);

        Long id = notification.getId();

        EntityManager em1 = entityManagerFactory.createEntityManager();
        EntityManager em2 = entityManagerFactory.createEntityManager();

        EntityTransaction tx1 = em1.getTransaction();
        EntityTransaction tx2 = em2.getTransaction();

        tx1.begin();
        tx2.begin();

        Notification first =
                em1.find(Notification.class, id);

        Notification second =
                em2.find(Notification.class, id);

        first.markAsRead();
        second.markAsRead();

        tx1.commit();

        Assertions.assertThrows(
                Exception.class,
                tx2::commit
        );

        em1.close();
        em2.close();
    }

    @Test
    void 처리중상태가30분이상지속되면복구대상이된다() {

        Notification notification =
                new Notification(
                        "민경",
                        "처리중",
                        "EVENT_PROCESSING_TIMEOUT",
                        NotificationStatus.REQUESTED,
                        NotificationChannel.EMAIL
                );

        notification.markAsProcessing();

        notification.forceProcessingStartedAt(
                LocalDateTime.now().minusMinutes(31)
        );

        assertThat(
                notification.isProcessingTooLong()
        ).isTrue();
    }

}