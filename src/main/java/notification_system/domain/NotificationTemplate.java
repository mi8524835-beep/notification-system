package notification_system.domain;

public enum NotificationTemplate {

    PAYMENT_SUCCESS("결제가 완료되었습니다."),
    PAYMENT_FAIL("결제 실패"),
    LECTURE_START("강의 시작");

    private final String message;

    NotificationTemplate(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}