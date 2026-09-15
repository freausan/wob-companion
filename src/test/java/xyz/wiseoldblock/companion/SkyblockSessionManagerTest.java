package xyz.wiseoldblock.companion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.wiseoldblock.companion.api.WiseOldBlockApiClient;
import xyz.wiseoldblock.companion.config.ModConfig;
import xyz.wiseoldblock.companion.session.SessionState;
import xyz.wiseoldblock.companion.session.SkyblockSessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class SkyblockSessionManagerTest {

    private HttpServer server;
    private int serverPort;
    private final CopyOnWriteArrayList<String> eventsReceived = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = server.getAddress().getPort();

        server.createContext("/players/", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            eventsReceived.add(new String(bytes, StandardCharsets.UTF_8));

            byte[] resp = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
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
    void testInitialState() {
        ModConfig config = new ModConfig();
        config.setBackendUrl("http://127.0.0.1:" + serverPort);
        WiseOldBlockApiClient client = new WiseOldBlockApiClient(config);
        SkyblockSessionManager manager = new SkyblockSessionManager(client, config);

        assertEquals(SessionState.NOT_IN_SKYBLOCK, manager.getCurrentState());
        assertNull(manager.getActivePlayerUuid());

        client.shutdown();
    }

    @Test
    void testStateTransitions() {
        ModConfig config = new ModConfig();
        config.setBackendUrl("http://127.0.0.1:" + serverPort);
        WiseOldBlockApiClient client = new WiseOldBlockApiClient(config);
        SkyblockSessionManager manager = new SkyblockSessionManager(client, config);

        UUID uuid = UUID.randomUUID();

        // Simulate transitioning to IN_SKYBLOCK
        manager.setCurrentStateForTesting(SessionState.IN_SKYBLOCK, uuid);
        assertEquals(SessionState.IN_SKYBLOCK, manager.getCurrentState());
        assertEquals(uuid, manager.getActivePlayerUuid());

        // Simulate warp grace period entry
        manager.setCurrentStateForTesting(SessionState.WARP_GRACE_PERIOD, uuid);
        assertEquals(SessionState.WARP_GRACE_PERIOD, manager.getCurrentState());

        client.shutdown();
    }

    @Test
    void testMidSessionUpdateIntervalConstant() {
        assertEquals(10_000L, SkyblockSessionManager.GRACE_PERIOD_MILLIS);
        assertEquals(30 * 60 * 1000L, SkyblockSessionManager.MID_SESSION_UPDATE_INTERVAL_MILLIS);
    }
}