package cc.infrai.fintech;

import cc.infrai.fintech.config.ServiceConfig;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentOutcome;
import cc.infrai.fintech.infra.InfraiClient;
import cc.infrai.fintech.infra.QueueMessage;
import cc.infrai.fintech.service.PaymentNotificationService;

import java.io.IOException;
import java.util.List;

public class NotificationDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServiceConfig config = ServiceConfig.fromEnv();
        InfraiClient infrai = new InfraiClient(config.apiKey(), config.baseUrl());
        PaymentNotificationService service = new PaymentNotificationService(infrai, config.queueName(), config.rtcRoomName());

        String roomName = service.ensureNotificationRoom();
        String rtcToken = service.issueUserStreamToken("user_42", "Alice Ledger");

        PaymentEvent payment = new PaymentEvent(
                "pay_1001",
                "user_42",
                2450L,
                "USD",
                "COFFEE-SHOP-4401",
                false,
                false
        );

        PaymentOutcome outcome = service.processPayment(payment);

        System.out.println("room=" + roomName);
        System.out.println("rtcTokenIssued=" + (!rtcToken.isBlank()));
        System.out.println("action=" + outcome.action());
        System.out.println("queued=" + outcome.queued());

        if (outcome.queued()) {
            List<QueueMessage> messages = service.pullQueuedNotifications(1, 30);
            for (QueueMessage message : messages) {
                System.out.println("queuedMessageId=" + message.messageId());
                System.out.println("queuedPayload=" + message.payload());
            }
        }
    }
}
