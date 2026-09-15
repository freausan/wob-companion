package xyz.wiseoldblock.companion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.wiseoldblock.companion.api.ApiResponse;
import xyz.wiseoldblock.companion.api.SessionEvent;
import xyz.wiseoldblock.companion.api.WiseOldBlockApiClient;
import xyz.wiseoldblock.companion.config.ModConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class WiseOldBlockApiClientTest {

    private HttpServer server;
    private int serverPort;
    private final AtomicReference<String> lastReceivedPath = new AtomicReference<>();
    private final AtomicReference<String> lastReceivedBody = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{\"success\":true}");

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = server.getAddress().getPort();

        server.createContext("/players/", exchange -> {
            lastReceivedPath.set(exchange.getRequestURI().getPath());
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            lastReceivedBody.set(new String(bytes, StandardCharsets.UTF_8));

            byte[] resp = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testFormatUuid() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals("069a79f444e94726a5befca90e38aaf5", WiseOldBlockApiClient.formatUuid(uuid));
    }

    @Test
    void testSendConnectEventAsyncSuccess() {
        ModConfig config = new ModConfig();
        config.setBackendUrl("http://127.0.0.1:" + serverPort);
        WiseOldBlockApiClient client = new WiseOldBlockApiClient(config);

        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        responseStatus.set(200);
        responseBody.set("{\"status\":\"ok\"}");

        ApiResponse response = client.sendSessionEventAsync(uuid, SessionEvent.CONNECT).join();

        assertEquals(200, response.statusCode());
        assertTrue(response.success());
        assertFalse(response.cooldownActive());
        assertEquals("/players/069a79f444e94726a5befca90e38aaf5", lastReceivedPath.get());
        assertTrue(lastReceivedBody.get().contains("\"event\":\"connect\""));

        client.shutdown();
    }

    @Test
    void testSendDisconnectEventSync202Accepted() {
        ModConfig config = new ModConfig();
        config.setBackendUrl("http://127.0.0.1:" + serverPort);
        WiseOldBlockApiClient client = new WiseOldBlockApiClient(config);

        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        responseStatus.set(202);
        responseBody.set("{\"queued\":true,\"event\":\"disconnect\",\"delaySeconds\":90}");

        ApiResponse response = client.sendSessionEventSync(uuid, SessionEvent.DISCONNECT, Duration.ofSeconds(2));

        assertEquals(202, response.statusCode());
        assertTrue(response.success());
        assertEquals("/players/069a79f444e94726a5befca90e38aaf5", lastReceivedPath.get());
        assertTrue(lastReceivedBody.get().contains("\"event\":\"disconnect\""));

        client.shutdown();
    }

    @Test
    void testSendConnectEventCooldown429() {
        ModConfig config = new ModConfig();
        config.setBackendUrl("http://127.0.0.1:" + serverPort);
        WiseOldBlockApiClient client = new WiseOldBlockApiClient(config);

        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        responseStatus.set(429);
        responseBody.set("{\"error\":\"Player was updated recently.\",\"retryAfterSeconds\":600}");

        ApiResponse response = client.sendSessionEventAsync(uuid, SessionEvent.CONNECT).join();

        assertEquals(429, response.statusCode());
        assertFalse(response.success());
        assertTrue(response.cooldownActive());

        client.shutdown();
    }
}