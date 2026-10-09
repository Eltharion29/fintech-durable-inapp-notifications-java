package cc.infrai.fintech.domain;

public record PaymentOutcome(
        NotificationAction action,
        boolean queued,
        String auditEntry,
        PaymentNotification notification
) {
}
