package xyz.wiseoldblock.companion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.wiseoldblock.companion.api.WOBCompanionApiClient;
import xyz.wiseoldblock.companion.config.WOBCompanionConfig;

public class WOBCompanionMod implements ClientModInitializer {
    public static final String MOD_ID = "wob_companion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static WOBCompanionConfig config;
    private static WOBCompanionApiClient apiClient;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing WOB Companion (Fabric 26.1)...");

        config = WOBCompanionConfig.load();
        apiClient = new WOBCompanionApiClient(config);

        // Connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Connected to server");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Disconnected from server");
        });

        // Register client commands: /wob and /companion
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher, "wob");
            registerCommands(dispatcher, "companion");
        });

        LOGGER.info("WOB Companion mod initialized successfully.");
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher, String commandName) {
        dispatcher.register(
            ClientCommands.literal(commandName)
                .executes(context -> {
                    sendStatus(context.getSource());
                    return 1;
                })
                .then(ClientCommands.literal("status").executes(context -> {
                    sendStatus(context.getSource());
                    return 1;
                }))
                .then(ClientCommands.literal("ping").executes(context -> {
                    context.getSource().sendFeedback(Component.literal("§e[WOB Companion] Pinging Wise Old Block API..."));
                    apiClient.pingAsync().thenAccept(success -> {
                        if (success) {
                            context.getSource().sendFeedback(Component.literal("§a[WOB Companion] Wise Old Block API is reachable!"));
                        } else {
                            context.getSource().sendFeedback(Component.literal("§c[WOB Companion] Could not reach Wise Old Block API. Check your internet or endpoint setting."));
                        }
                    });
                    return 1;
                }))
                .then(ClientCommands.literal("toggle").executes(context -> {
                    boolean newState = !config.isEnabled();
                    config.setEnabled(newState);
                    config.save();
                    context.getSource().sendFeedback(Component.literal("§6[WOB Companion] Mod is now " + (newState ? "§aENABLED" : "§cDISABLED")));
                    return 1;
                }))
                .then(ClientCommands.literal("help").executes(context -> {
                    context.getSource().sendFeedback(Component.literal("§b--- WOB Companion Commands ---"));
                    context.getSource().sendFeedback(Component.literal("§e/" + commandName + " status §7- View connection and config status"));
                    context.getSource().sendFeedback(Component.literal("§e/" + commandName + " ping   §7- Test connection to API"));
                    context.getSource().sendFeedback(Component.literal("§e/" + commandName + " toggle §7- Enable or disable companion"));
                    return 1;
                }))
        );
    }

    private static void sendStatus(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("§b--- WOB Companion Status ---"));
        source.sendFeedback(Component.literal("§7Status: " + (config.isEnabled() ? "§aEnabled" : "§cDisabled")));
        source.sendFeedback(Component.literal("§7API Base URL: §f" + config.getApiBaseUrl()));
        source.sendFeedback(Component.literal("§7API Key Set: " + (!config.getApiKey().isBlank() ? "§aYes" : "§7No")));
        source.sendFeedback(Component.literal("§7Type §e/wob help§7 or §e/companion help§7 for command list."));
    }

    public static WOBCompanionConfig getConfig() {
        return config;
    }

    public static WOBCompanionApiClient getApiClient() {
        return apiClient;
    }
}
