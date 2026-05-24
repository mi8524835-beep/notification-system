package notification_system.domain;

public enum NotificationTemplate {

    PAYMENT_SUCCESS("결제가 완료되었습니다."),
    PAYMENT_FAIL("결제에 실패했습니다."),
    LECTURE_START("강의 시작 하루 전입니다.");

    private final String message;

    NotificationTemplate(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
