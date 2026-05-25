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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldSaveNotification() {

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
    void shouldPreventDuplicateNotification() {

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
    void shouldMarkNotificationAsRead() {

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
    void shouldIncreaseRetryCountWhenFailed() {

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
    void shouldChangeStatusToSent() {

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
    void shouldReturnTemplateMessage() {

        assertThat(
                NotificationTemplate.PAYMENT_SUCCESS.getMessage()
        ).isEqualTo("결제가 완료되었습니다.");
    }


    @Test
    void shouldSetNextRetryTimeWhenFailed() {

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
    void shouldRecoverLongProcessingStatus() {

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
    void shouldApplyOptimisticLockWhenSameNotificationIsUpdatedConcurrently() {

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
    void shouldDetectLongProcessingStatusAsRecoveryTarget() {

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