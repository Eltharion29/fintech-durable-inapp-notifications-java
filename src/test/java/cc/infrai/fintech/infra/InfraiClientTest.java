package cc.infrai.fintech.infra;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfraiClientTest {
    private HttpServer server;
    private final List<String> requestBodies = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/queue/publish", exchange -> respond(exchange, "{\"ok\":true,\"data\":{}}"));
        server.createContext("/v1/queue/consume", exchange -> respond(exchange, "{\"ok\":true,\"data\":{\"messages\":[]}}"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void queueRequestsIncludeQueueAsATopLevelField() throws Exception {
        InfraiClient client = new InfraiClient("test-key", "http://localhost:" + server.getAddress().getPort());

        client.queuePublish("notifications", "{\"notification_id\":\"notif-1\"}");
        client.queueConsume("notifications", 1, 30);

        assertEquals("{\"queue\":\"notifications\",\"payload\":\"{\\\"notification_id\\\":\\\"notif-1\\\"}\"}", requestBodies.get(0));
        assertEquals("{\"queue\":\"notifications\",\"max_messages\":1,\"visibility_timeout\":30}", requestBodies.get(1));
    }

    private void respond(HttpExchange exchange, String response) throws IOException {
        requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
