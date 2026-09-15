package xyz.wiseoldblock.companion.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wiseoldblock.companion.config.WOBCompanionConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for communicating with the Wise Old Block API.
 */
public class WOBCompanionApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("wob_companion");

    private final WOBCompanionConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public WOBCompanionApiClient(WOBCompanionConfig config) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "wob-companion-http");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executor)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Checks connection to the Wise Old Block API.
     */
    public CompletableFuture<Boolean> pingAsync() {
        String endpoint = config.getApiBaseUrl();
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        endpoint += "health";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("User-Agent", "WOBCompanion/1.0.0 (Fabric 26.1)")
                .timeout(Duration.ofSeconds(10))
                .GET();

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> resp.statusCode() >= 200 && resp.statusCode() < 400)
                .exceptionally(ex -> {
                    LOGGER.warn("Failed to ping Wise Old Block API: {}", ex.getMessage());
                    return false;
                });
    }

    /**
     * Sends an async GET request to a relative path on the API.
     */
    public CompletableFuture<String> getAsync(String relativePath) {
        String base = config.getApiBaseUrl();
        if (!base.endsWith("/")) {
            base += "/";
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + relativePath))
                .header("User-Agent", "WOBCompanion/1.0.0 (Fabric 26.1)")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET();

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
