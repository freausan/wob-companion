package xyz.wiseoldblock.companion.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import xyz.wiseoldblock.companion.config.ModConfig;
import xyz.wiseoldblock.companion.session.SkyblockSessionManager;

/**
 * In-game client command registration for /wob and /companion.
 */
public class WOBCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                ModConfig config,
                                SkyblockSessionManager sessionManager) {
        registerRoot(dispatcher, "wob", config, sessionManager);
        registerRoot(dispatcher, "companion", config, sessionManager);
    }

    private static void registerRoot(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                     String rootName,
                                     ModConfig config,
                                     SkyblockSessionManager sessionManager) {
        dispatcher.register(
            ClientCommands.literal(rootName)
                .executes(context -> {
                    sendStatus(context.getSource(), config, sessionManager);
                    return 1;
                })
                .then(ClientCommands.literal("status").executes(context -> {
                    sendStatus(context.getSource(), config, sessionManager);
                    return 1;
                }))
                .then(ClientCommands.literal("update").executes(context -> {
                    sessionManager.forceUpdate(context.getSource().getClient());
                    return 1;
                }))
                .then(ClientCommands.literal("toggle").executes(context -> {
                    boolean newState = !config.isEnabled();
                    config.setEnabled(newState);
                    config.save();
                    context.getSource().sendFeedback(Component.literal(
                        "§b[Wise Old Block] §7Companion tracking is now " + (newState ? "§aENABLED" : "§cDISABLED")));
                    return 1;
                }))
                .then(ClientCommands.literal("notify").executes(context -> {
                    boolean newNotify = !config.isNotifyInChat();
                    config.setNotifyInChat(newNotify);
                    config.save();
                    context.getSource().sendFeedback(Component.literal(
                        "§b[Wise Old Block] §7Chat notifications are now " + (newNotify ? "§aENABLED" : "§cDISABLED")));
                    return 1;
                }))
                .then(ClientCommands.literal("help").executes(context -> {
                    sendHelp(context.getSource(), rootName);
                    return 1;
                }))
        );
    }

    private static void sendStatus(FabricClientCommandSource source, ModConfig config, SkyblockSessionManager sessionManager) {
        source.sendFeedback(Component.literal("§b--- Wise Old Block Companion ---"));
        source.sendFeedback(Component.literal("§7Tracking Status: " + (config.isEnabled() ? "§aEnabled" : "§cDisabled")));
        source.sendFeedback(Component.literal("§7Backend URL: §f" + config.getBackendUrl()));
        source.sendFeedback(Component.literal("§7Chat Notifications: " + (config.isNotifyInChat() ? "§aEnabled" : "§7Disabled")));
        source.sendFeedback(Component.literal("§7Session State: §e" + sessionManager.getCurrentState()));
        if (sessionManager.getActivePlayerUuid() != null) {
            source.sendFeedback(Component.literal("§7Active Player UUID: §f" + sessionManager.getActivePlayerUuid()));
        }
        source.sendFeedback(Component.literal("§7Type §e/wob help §7for command options."));
    }

    private static void sendHelp(FabricClientCommandSource source, String root) {
        source.sendFeedback(Component.literal("§b--- Wise Old Block Commands ---"));
        source.sendFeedback(Component.literal("§e/" + root + " status  §7- Display connection and session state"));
        source.sendFeedback(Component.literal("§e/" + root + " update  §7- Manually request an immediate stats snapshot"));
        source.sendFeedback(Component.literal("§e/" + root + " toggle  §7- Enable/disable session tracking"));
        source.sendFeedback(Component.literal("§e/" + root + " notify  §7- Toggle in-game chat messages"));
    }
}