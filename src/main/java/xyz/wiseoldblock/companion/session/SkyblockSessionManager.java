package xyz.wiseoldblock.companion.session;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wiseoldblock.companion.api.ApiResponse;
import xyz.wiseoldblock.companion.api.SessionEvent;
import xyz.wiseoldblock.companion.api.WiseOldBlockApiClient;
import xyz.wiseoldblock.companion.config.ModConfig;
import xyz.wiseoldblock.companion.util.ChatFeedback;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/**
 * Manages Hypixel SkyBlock session lifecycle, state transitions,
 * and the warping / server-hopping grace period.
 */
public class SkyblockSessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("wob_companion");

    public static final long GRACE_PERIOD_MILLIS = 10_000L; // 10-second debounce
    public static final long MID_SESSION_UPDATE_INTERVAL_MILLIS = 30 * 60 * 1000L; // 30 minutes

    private final WiseOldBlockApiClient apiClient;
    private final ModConfig config;

    private SessionState currentState = SessionState.NOT_IN_SKYBLOCK;
    private UUID activePlayerUuid = null;
    private long gracePeriodStartMillis = 0L;
    private long lastSnapshotUpdateMillis = 0L;
    private int tickCounter = 0;

    public SkyblockSessionManager(WiseOldBlockApiClient apiClient, ModConfig config) {
        this.apiClient = apiClient;
        this.config = config;
    }

    /**
     * Called every client tick by ClientTickEvents.END_CLIENT_TICK.
     */
    public void onClientTick(Minecraft client) {
        // Run check every 5 ticks (250ms) to conserve client CPU
        if (++tickCounter % 5 != 0) {
            return;
        }

        boolean inSkyblock = isSkyBlock(client);

        switch (currentState) {
            case NOT_IN_SKYBLOCK -> {
                if (inSkyblock) {
                    onEnterSkyblock(client);
                }
            }
            case IN_SKYBLOCK -> {
                if (!inSkyblock) {
                    // Possible warp, server hop, or exit - start 10s grace period
                    currentState = SessionState.WARP_GRACE_PERIOD;
                    gracePeriodStartMillis = System.currentTimeMillis();
                    LOGGER.debug("Lost SkyBlock indicator; entering {}ms warping grace period", GRACE_PERIOD_MILLIS);
                }
            }
            case WARP_GRACE_PERIOD -> {
                if (inSkyblock) {
                    // Reconnected to a SkyBlock server within the 10-second window!
                    onWarpConfirmed(client);
                } else {
                    // Check if the 10-second debounce window has elapsed
                    long elapsed = System.currentTimeMillis() - gracePeriodStartMillis;
                    if (elapsed >= GRACE_PERIOD_MILLIS) {
                        LOGGER.info("Warp grace period expired ({}ms); player has exited SkyBlock", elapsed);
                        triggerDisconnect(client, false);
                    }
                }
            }
        }
    }

    /**
     * Handles transition from NOT_IN_SKYBLOCK -> IN_SKYBLOCK.
     */
    private void onEnterSkyblock(Minecraft client) {
        if (client.player == null) {
            return;
        }

        activePlayerUuid = client.player.getUUID();
        currentState = SessionState.IN_SKYBLOCK;
        lastSnapshotUpdateMillis = System.currentTimeMillis();

        LOGGER.info("Player entered SkyBlock session (UUID: {})", activePlayerUuid);

        apiClient.sendSessionEventAsync(activePlayerUuid, SessionEvent.CONNECT)
                .thenAccept(response -> {
                    if (response.cooldownActive()) {
                        ChatFeedback.cooldownActive(client, config);
                    } else if (response.success()) {
                        ChatFeedback.sessionStarted(client, config);
                    }
                });
    }

    /**
     * Handles returning to SkyBlock during the 10-second warp grace period.
     */
    private void onWarpConfirmed(Minecraft client) {
        currentState = SessionState.IN_SKYBLOCK;
        LOGGER.debug("Warp confirmed within grace period; session maintained without interruption");

        // Mid-session update requirement:
        // If at least 30 minutes have elapsed since the last update, trigger a snapshot upon hopping.
        long now = System.currentTimeMillis();
        if (now - lastSnapshotUpdateMillis >= MID_SESSION_UPDATE_INTERVAL_MILLIS) {
            LOGGER.info("30-minute threshold reached on server hop; requesting mid-session snapshot");
            lastSnapshotUpdateMillis = now;
            if (activePlayerUuid != null) {
                apiClient.sendSessionEventAsync(activePlayerUuid, SessionEvent.CONNECT)
                        .thenAccept(response -> {
                            if (response.success()) {
                                ChatFeedback.midSessionUpdate(client, config);
                            }
                        });
            }
        }
    }

    /**
     * Handles disconnection or leaving SkyBlock after grace period.
     */
    private void triggerDisconnect(Minecraft client, boolean isStopping) {
        currentState = SessionState.NOT_IN_SKYBLOCK;

        UUID playerUuid = activePlayerUuid;
        if (playerUuid == null && client.getUser() != null) {
            playerUuid = client.getUser().getProfileId();
        }

        if (playerUuid != null) {
            LOGGER.info("Ending SkyBlock session for player {}", playerUuid);

            if (isStopping) {
                // Synchronous wait on client stopping so the HTTP packet is dispatched before process dies
                apiClient.sendSessionEventSync(playerUuid, SessionEvent.DISCONNECT, Duration.ofSeconds(2));
            } else {
                apiClient.sendSessionEventAsync(playerUuid, SessionEvent.DISCONNECT)
                        .thenAccept(response -> {
                            if (response.success()) {
                                ChatFeedback.sessionEnded(client, config);
                            }
                        });
            }
        }

        activePlayerUuid = null;
    }

    /**
     * Called when the player disconnects from the multiplayer server.
     */
    public void onServerDisconnect(Minecraft client) {
        if (currentState != SessionState.NOT_IN_SKYBLOCK) {
            LOGGER.info("Server disconnected; concluding SkyBlock session");
            triggerDisconnect(client, false);
        }
    }

    /**
     * Called when the Minecraft client is stopping / shutting down.
     */
    public void onClientStopping(Minecraft client) {
        if (currentState != SessionState.NOT_IN_SKYBLOCK) {
            LOGGER.info("Client stopping; flushing SkyBlock disconnect event");
            triggerDisconnect(client, true);
        }
        apiClient.shutdown();
    }

    /**
     * Manually triggers a session snapshot update (e.g. from `/wob update`).
     */
    public void forceUpdate(Minecraft client) {
        if (client.player == null) {
            ChatFeedback.sendDirect(client, "§cYou must be logged into a world to update.");
            return;
        }

        UUID uuid = client.player.getUUID();
        ChatFeedback.sendDirect(client, "§eRequesting player snapshot from Wise Old Block API...");

        apiClient.sendSessionEventAsync(uuid, SessionEvent.CONNECT)
                .thenAccept(response -> {
                    if (response.cooldownActive()) {
                        ChatFeedback.sendDirect(client, "§cProfile updated recently (cooldown active: <15m).");
                    } else if (response.success()) {
                        lastSnapshotUpdateMillis = System.currentTimeMillis();
                        ChatFeedback.sendDirect(client, "§aSnapshot created successfully!");
                    } else {
                        ChatFeedback.sendDirect(client, "§cFailed to record snapshot: " + response.errorMessage());
                    }
                });
    }

    /**
     * Verifies that the client is connected to a Hypixel server (*.hypixel.net).
     */
    public static boolean isHypixelConnected(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null) {
            if (server.ip.toLowerCase().contains("hypixel.net")) {
                return true;
            }
        }

        if (client.getConnection() != null && client.getConnection().getServerData() != null) {
            String ip = client.getConnection().getServerData().ip;
            if (ip != null && ip.toLowerCase().contains("hypixel.net")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the player is currently in Hypixel SkyBlock.
     * Evaluates scoreboard sidebar title ("SKYBLOCK") and auxiliary lines.
     */
    public static boolean isSkyBlock(Minecraft client) {
        if (!isHypixelConnected(client) || client.level == null || client.player == null) {
            return false;
        }

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) {
            return false;
        }

        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return false;
        }

        String title = sidebar.getDisplayName().getString().trim().toUpperCase();
        if (title.contains("SKYBLOCK")) {
            return true;
        }

        // Secondary verification: inspect score lines for typical SkyBlock indicators
        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(sidebar);
        for (PlayerScoreEntry entry : scores) {
            String line = entry.ownerName().getString().toUpperCase();
            if (line.contains("PURSE:") || line.contains("BITS:") || line.contains("COINS:")) {
                return true;
            }
        }

        return false;
    }

    public SessionState getCurrentState() {
        return currentState;
    }

    public UUID getActivePlayerUuid() {
        return activePlayerUuid;
    }

    public long getLastSnapshotUpdateMillis() {
        return lastSnapshotUpdateMillis;
    }

    public void setLastSnapshotUpdateMillis(long millis) {
        this.lastSnapshotUpdateMillis = millis;
    }

    public void setCurrentStateForTesting(SessionState state, UUID uuid) {
        this.currentState = state;
        this.activePlayerUuid = uuid;
    }
}