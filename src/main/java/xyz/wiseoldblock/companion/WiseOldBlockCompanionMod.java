package xyz.wiseoldblock.companion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wiseoldblock.companion.api.WiseOldBlockApiClient;
import xyz.wiseoldblock.companion.command.WOBCommands;
import xyz.wiseoldblock.companion.config.ModConfig;
import xyz.wiseoldblock.companion.session.SkyblockSessionManager;

/**
 * Main client entrypoint for Wise Old Block Companion mod.
 */
public class WiseOldBlockCompanionMod implements ClientModInitializer {
    public static final String MOD_ID = "wob_companion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;
    private static WiseOldBlockApiClient apiClient;
    private static SkyblockSessionManager sessionManager;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Wise Old Block Companion (Fabric 26.1.2)...");

        config = ModConfig.load();
        apiClient = new WiseOldBlockApiClient(config);
        sessionManager = new SkyblockSessionManager(apiClient, config);

        // 1. Tick loop: driving session state transitions and 10s warping grace period
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            sessionManager.onClientTick(client);
        });

        // 2. Server disconnect: concluding session
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Server disconnect event received");
            sessionManager.onServerDisconnect(client);
        });

        // 3. Client lifecycle stopping: flush session disconnect event
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            LOGGER.debug("Client stopping lifecycle event received");
            sessionManager.onClientStopping(client);
        });

        // 4. JVM Shutdown Hook backup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (sessionManager != null) {
                    apiClient.shutdown();
                }
            } catch (Exception ignored) {
            }
        }, "wob-shutdown-hook"));

        // 5. In-game client command registration: /wob and /companion
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            WOBCommands.register(dispatcher, config, sessionManager);
        });

        LOGGER.info("Wise Old Block Companion initialized successfully. Backend URL: {}", config.getBackendUrl());
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static WiseOldBlockApiClient getApiClient() {
        return apiClient;
    }

    public static SkyblockSessionManager getSessionManager() {
        return sessionManager;
    }
}