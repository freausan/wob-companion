package xyz.wiseoldblock.companion.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xyz.wiseoldblock.companion.config.ModConfig;

/**
 * Utility for formatting and sending clean client-side chat feedback.
 */
public class ChatFeedback {
    public static final String PREFIX = "§b[Wise Old Block] §7";

    public static void sendMessage(Minecraft client, ModConfig config, String message) {
        if (!config.isNotifyInChat()) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(PREFIX + message));
            }
        });
    }

    public static void sendDirect(Minecraft client, String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(PREFIX + message));
            }
        });
    }

    public static void sessionStarted(Minecraft client, ModConfig config) {
        sendMessage(client, config, "SkyBlock session started.");
    }

    public static void sessionEnded(Minecraft client, ModConfig config) {
        sendMessage(client, config, "SkyBlock session ended.");
    }

    public static void cooldownActive(Minecraft client, ModConfig config) {
        sendMessage(client, config, "Profile updated recently (cooldown active).");
    }
}