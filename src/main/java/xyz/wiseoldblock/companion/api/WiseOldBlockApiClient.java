package xyz.wiseoldblock.companion.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wiseoldblock.companion.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous HTTP client communicating with the Wise Old Block REST API.
 */
public class WiseOldBlockApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("wob_companion");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final ModConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public WiseOldBlockApiClient(ModConfig config) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "wob-api-client");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     * Sends a session boundary event (connect or disconnect) asynchronously to the backend.
     *
     * @param playerUuid Player UUID (can be formatted with or without dashes)
     * @param event      SessionEvent (CONNECT or DISCONNECT)
     * @return CompletableFuture of ApiResponse
     */
    public CompletableFuture<ApiResponse> sendSessionEventAsync(UUID playerUuid, SessionEvent event) {
        if (!config.isEnabled()) {
            LOGGER.debug("Wise Old Block companion is disabled; skipping {} event for {}", event.getEventName(), playerUuid);
            return CompletableFuture.completedFuture(ApiResponse.error(0, "Mod disabled in config", ""));
        }

        HttpRequest request = buildRequest(playerUuid, event, REQUEST_TIMEOUT);
        LOGGER.info("Sending {} session event for player {} to {}", event.getEventName(), playerUuid, request.uri());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processResponse(response, event, playerUuid))
                .exceptionally(throwable -> {
                    LOGGER.warn("Failed to send {} event for {}: {}", event.getEventName(), playerUuid, throwable.getMessage());
                    return ApiResponse.error(0, throwable.getMessage(), "");
                });
    }

    /**
     * Sends a session boundary event synchronously with a bounded wait timeout.
     * Used exclusively during JVM/client shutdown to ensure the disconnect request is delivered.
     */
    public ApiResponse sendSessionEventSync(UUID playerUuid, SessionEvent event, Duration maxWait) {
        if (!config.isEnabled()) {
            return ApiResponse.error(0, "Mod disabled in config", "");
        }

        try {
            HttpRequest request = buildRequest(playerUuid, event, maxWait);
            LOGGER.info("Flushing synchronous {} session event for player {} on shutdown", event.getEventName(), playerUuid);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return processResponse(response, event, playerUuid);
        } catch (Exception e) {
            LOGGER.warn("Synchronous {} event failed during shutdown: {}", event.getEventName(), e.getMessage());
            return ApiResponse.error(0, e.getMessage(), "");
        }
    }

    private HttpRequest buildRequest(UUID playerUuid, SessionEvent event, Duration timeout) {
        String base = config.getBackendUrl();
        String formattedUuid = formatUuid(playerUuid);
        String url = base + "/players/" + formattedUuid;
        String jsonPayload = "{\"event\":\"" + event.getEventName() + "\"}";

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", "WiseOldBlock-Companion/1.0.0 (Fabric 26.1.2)")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
    }

    private ApiResponse processResponse(HttpResponse<String> response, SessionEvent event, UUID playerUuid) {
        int statusCode = response.statusCode();
        String body = response.body();

        if (statusCode == 200) {
            LOGGER.info("Successfully recorded {} event for player {}: {}", event.getEventName(), playerUuid, body);
            return ApiResponse.success(statusCode, body);
        } else if (statusCode == 202) {
            LOGGER.info("Backend scheduled delayed disconnect processing for player {}: {}", playerUuid, body);
            return ApiResponse.success(statusCode, body);
        } else if (statusCode == 429) {
            LOGGER.info("Snapshot cooldown active for player {} (HTTP 429): {}", playerUuid, body);
            return ApiResponse.cooldown(statusCode, body);
        } else {
            LOGGER.warn("Backend returned unexpected HTTP {} for player {}: {}", statusCode, playerUuid, body);
            return ApiResponse.error(statusCode, "HTTP " + statusCode, body);
        }
    }

    /**
     * Formats UUID without hyphens (32-character hex representation),
     * matching Hypixel SkyBlock API and Wise Old Block conventions.
     */
    public static String formatUuid(UUID uuid) {
        return uuid.toString().replace("-", "").toLowerCase();
    }

    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}