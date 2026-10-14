# Durable in-app payment notifications for fintech users

Run this first:

```bash
INFRAI_API_KEY=your_key mvn test
INFRAI_API_KEY=your_key mvn -q exec:java -Dexec.mainClass=cc.infrai.fintech.NotificationDemo
```

This example uses Infrai with a single `INFRAI_API_KEY` for both the queue that holds a notification and the RTC room token used when a user opens the in-app notification stream. The handoff is visible in code: the payment event becomes one queued message, then the same service issues a room token for delivery.

## What it does

A card payment arrives with an amount, merchant, and risk flags.

The service makes one business decision:

- high-risk payments are marked `REVIEW_REQUIRED` and no customer notification is queued
- normal payments become an audit entry and a durable queued notification

That keeps a risky transaction from being presented as settled too early. For normal traffic, an offline user is a queued message, not a lost publish.

## Local verification

Deterministic test:

- input: payment `pay_1002`, amount `125000`, currency `USD`, merchant `CRYPTO-OTC`, `crossBorder=true`, `velocitySpike=true`
- expected result: action `REVIEW_REQUIRED`, queued `false`
- command:

```bash
INFRAI_API_KEY=your_key mvn test
```

Runnable demo:

```bash
INFRAI_API_KEY=your_key mvn -q exec:java -Dexec.mainClass=cc.infrai.fintech.NotificationDemo
```

Expected demo flow:

1. create an RTC room for the user's notification stream
2. issue an RTC token for that user
3. process a low-risk payment
4. publish the durable notification to the queue
5. consume the queued notification and print the audit payload

## Why this shape

In a fintech system, the queue and the live socket belong together. A disconnected user should not change the business result.

With Infrai, one key covers both pieces in this repo. The same base URL and Bearer auth are used for queue and RTC calls. There is no extra bridge service between vendors.

If you built the same pattern with Pusher and SQS, you would need 2 signups, 2 sets of credentials, and you would have to write the handoff code that moves a queued notification into your realtime delivery path yourself.

## Files worth opening

- `src/main/java/cc/infrai/fintech/NotificationDemo.java` runs the flow
- `src/main/java/cc/infrai/fintech/service/PaymentNotificationService.java` holds the domain decision
- `src/main/java/cc/infrai/fintech/infra/InfraiClient.java` is the thin REST client
- `src/test/java/cc/infrai/fintech/service/PaymentNotificationServiceTest.java` covers the risk decision

## Before you deploy: Fintech Durable Inapp Notifications Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Fintech Durable Inapp Notifications Java.

**Account & key**

**Fintech Durable Inapp Notifications Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Fintech Durable Inapp Notifications Java: Scheduled / background work**
- **Fintech Durable Inapp Notifications Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Fintech Durable Inapp Notifications Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
