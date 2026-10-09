package cc.infrai.fintech.service;

import cc.infrai.fintech.domain.NotificationAction;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNotificationServiceTest {
    @Test
    void highRiskPaymentRequiresReviewInsteadOfCustomerNotification() throws Exception {
        PaymentEvent payment = new PaymentEvent(
                "pay_1002",
                "user_77",
                125000L,
                "USD",
                "CRYPTO-OTC",
                true,
                true
        );

        PaymentOutcome outcome = new PaymentNotificationService(null, "fintech-user-notifications", "fintech-user-notification-room") {
        }.processPayment(payment);

        assertEquals(NotificationAction.REVIEW_REQUIRED, outcome.action());
        assertFalse(outcome.queued());
        assertNull(outcome.notification());
        assertTrue(outcome.auditEntry().contains("decision=REVIEW_REQUIRED"));
    }
}
