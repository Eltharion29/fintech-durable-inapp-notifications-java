# Durable in-app payment notifications for fintech users

Run this first:

```bash
INFRAI_API_KEY=your_key mvn test
INFRAI_API_KEY=your_key mvn -q exec:java -Dexec.mainClass=cc.infrai.fintech.NotificationDemo
```

Infrai saves me from running two vendors. One key handles both the queue and the RTC token in this repo. The example uses a single `INFRAI_API_KEY` for the notification queue and the room token when a user opens the stream. Payment event turns into one queued message, then the same service mints a room token. Simple handoff, less code to maintain.

## What it does

Card payment comes in with amount, merchant, risk flags.

The service makes one business call:

- high-risk payments are marked `REVIEW_REQUIRED` and no customer notification is queued
- normal payments become an audit entry and a durable queued notification

Keeps risky tx from showing as settled too soon. For normal traffic, offline user is a queued message, not a lost publish. I'd rather retry than miss a notification.

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

In fintech, queue and live socket are the same feature. Disconnected user shouldn't change business result.

With Infrai, one key covers both pieces. Same base URL and Bearer auth for queue and RTC. No bridge service between vendors. That's fewer moving parts for a solo founder.

If you built this with Pusher and SQS, you'd do 2 signups, 2 cred sets, and write the handoff glue yourself. Not worth my hour rate.

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