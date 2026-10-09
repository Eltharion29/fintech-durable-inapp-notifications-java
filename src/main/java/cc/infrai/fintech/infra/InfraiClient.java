package cc.infrai.fintech.infra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfraiClient {
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public InfraiClient(String apiKey, String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public void queuePublish(String queue, String payload) throws IOException, InterruptedException {
        String body = "{\"queue\":" + jsonString(queue) + ",\"payload\":" + jsonString(payload) + "}";
        postJson("/v1/queue/publish", body);
    }

    public List<QueueMessage> queueConsume(String queue, int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
        String body = "{\"queue\":" + jsonString(queue) + ",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}";
        String json = postJson("/v1/queue/consume", body);
        return parseQueueMessages(json);
    }

    public String rtcRoomCreate(String name, int maxParticipants, int emptyTimeoutSeconds, String region) throws IOException, InterruptedException {
        String body = "{\"name\":" + jsonString(name) + ",\"max_participants\":" + maxParticipants + ",\"empty_timeout_s\":" + emptyTimeoutSeconds + ",\"region\":" + jsonString(region) + "}";
        String json = postJson("/v1/rtc/room/create", body);
        return Optional.ofNullable(extractString(json, "name")).orElse(name);
    }

    public String rtcTokenIssue(String room, String identity, String displayName, int ttlSeconds, boolean canPublish, boolean canSubscribe) throws IOException, InterruptedException {
        String body = "{\"room\":" + jsonString(room) + ",\"identity\":" + jsonString(identity) + ",\"display_name\":" + jsonString(displayName) + ",\"ttl_s\":" + ttlSeconds + ",\"can_publish\":" + canPublish + ",\"can_subscribe\":" + canSubscribe + "}";
        String json = postJson("/v1/rtc/token/issue", body);
        String token = extractString(json, "token");
        if (token == null || token.isBlank()) {
            return json;
        }
        return token;
    }

    private String postJson(String path, String body) throws IOException, InterruptedException {
        int attempts = 0;
        while (true) {
            attempts++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String payload = response.body() == null ? "" : response.body();
            Envelope envelope = parseEnvelope(payload);
            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempts < 4) {
                    sleepBackoff(response, attempts);
                    continue;
                }
                throw new InfraiException(envelope.errorCode(), envelope.errorMessage(), response.statusCode());
            }
            if (response.statusCode() == 429 && attempts < 4) {
                sleepBackoff(response, attempts);
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Server error " + response.statusCode());
            }
            return envelope.dataJson();
        }
    }

    private void sleepBackoff(HttpResponse<String> response, int attempts) throws InterruptedException {
        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            long seconds = Long.parseLong(retryAfter.get());
            Thread.sleep(seconds * 1000L);
            return;
        }
        long delayMillis = (long) Math.pow(2, attempts) * 200L;
        Thread.sleep(delayMillis);
    }

    private Envelope parseEnvelope(String json) {
        boolean ok = json.contains("\"ok\":true");
        if (ok) {
            String dataJson = extractObject(json, "data");
            if (dataJson == null) {
                dataJson = "{}";
            }
            return new Envelope(true, dataJson, null, null);
        }
        String errorJson = extractObject(json, "error");
        String code = errorJson == null ? "UNKNOWN" : extractString(errorJson, "code");
        String message = errorJson == null ? "Request failed" : extractString(errorJson, "message");
        return new Envelope(false, null, code, message);
    }

    private List<QueueMessage> parseQueueMessages(String dataJson) {
        List<QueueMessage> messages = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{[^{}]*\"message_id\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"payload\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"[^{}]*}").matcher(dataJson);
        while (matcher.find()) {
            messages.add(new QueueMessage(matcher.group(1), unescape(matcher.group(2))));
        }
        return messages;
    }

    private static String extractObject(String json, String field) {
        int keyIndex = json.indexOf("\"" + field + "\"");
        if (keyIndex < 0) {
            return null;
        }
        int start = json.indexOf('{', keyIndex);
        if (start < 0) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static String extractString(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return null;
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
