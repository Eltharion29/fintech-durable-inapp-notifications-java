package cc.infrai.fintech.domain;

public record PaymentEvent(
        String paymentId,
        String userId,
        long amountMinor,
        String currency,
        String merchant,
        boolean crossBorder,
        boolean velocitySpike
) {
}
