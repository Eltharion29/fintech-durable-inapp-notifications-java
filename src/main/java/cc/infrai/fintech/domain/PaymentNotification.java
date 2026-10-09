package cc.infrai.fintech.domain;

public record PaymentNotification(
        String notificationId,
        String paymentId,
        String userId,
        String message,
        String auditCategory
) {
}
