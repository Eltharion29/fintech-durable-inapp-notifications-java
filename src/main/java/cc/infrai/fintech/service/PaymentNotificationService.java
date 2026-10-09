package cc.infrai.fintech.service;

import cc.infrai.fintech.domain.NotificationAction;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentNotification;
import cc.infrai.fintech.domain.PaymentOutcome;
import cc.infrai.fintech.infra.InfraiClient;
import cc.infrai.fintech.infra.QueueMessage;

import java.io.IOException;
import java.util.List;

public class PaymentNotificationService {
    private final InfraiClient infrai;
    private final String queueName;
    private final String rtcRoomName;

    public PaymentNotificationService(InfraiClient infrai, String queueName, String rtcRoomName) {
        this.infrai = infrai;
        this.queueName = queueName;
        this.rtcRoomName = rtcRoomName;
    }

    public PaymentOutcome processPayment(PaymentEvent payment) throws IOException, InterruptedException {
        if (requiresReview(payment)) {
            String audit = "payment=" + payment.paymentId() + ";user=" + payment.userId() + ";decision=REVIEW_REQUIRED;merchant=" + payment.merchant();
            return new PaymentOutcome(NotificationAction.REVIEW_REQUIRED, false, audit, null);
        }

        PaymentNotification notification = new PaymentNotification(
                "notif-" + payment.paymentId(),
                payment.paymentId(),
                payment.userId(),
                "Payment posted: " + formatAmount(payment.amountMinor(), payment.currency()) + " at " + payment.merchant(),
                "PAYMENT_POSTED"
        );

        String audit = "payment=" + payment.paymentId() + ";user=" + payment.userId() + ";decision=QUEUED_FOR_DELIVERY;notification=" + notification.notificationId();
        String payload = "{"
                + "\"notification_id\":\"" + notification.notificationId() + "\"," 
                + "\"payment_id\":\"" + notification.paymentId() + "\"," 
                + "\"user_id\":\"" + notification.userId() + "\"," 
                + "\"message\":\"" + notification.message().replace("\"", "\\\"") + "\"," 
                + "\"audit\":\"" + audit + "\""
                + "}";
        infrai.queuePublish(queueName, payload);
        return new PaymentOutcome(NotificationAction.QUEUED_FOR_DELIVERY, true, audit, notification);
    }

    public String ensureNotificationRoom() throws IOException, InterruptedException {
        return infrai.rtcRoomCreate(rtcRoomName, 200, 120, "global");
    }

    public String issueUserStreamToken(String userId, String displayName) throws IOException, InterruptedException {
        return infrai.rtcTokenIssue(rtcRoomName, userId, displayName, 3600, false, true);
    }

    public List<QueueMessage> pullQueuedNotifications(int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
        return infrai.queueConsume(queueName, maxMessages, visibilityTimeout);
    }

    static boolean requiresReview(PaymentEvent payment) {
        return payment.amountMinor() >= 100000L || payment.crossBorder() || payment.velocitySpike();
    }

    private static String formatAmount(long amountMinor, String currency) {
        long major = amountMinor / 100;
        long minor = amountMinor % 100;
        return currency + " " + major + "." + String.format("%02d", minor);
    }
}
