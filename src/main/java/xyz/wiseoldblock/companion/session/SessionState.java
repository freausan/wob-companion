package xyz.wiseoldblock.companion.session;

/**
 * States of the SkyBlock player session state machine.
 */
public enum SessionState {
    /**
     * Player is not in SkyBlock (disconnected, singleplayer, non-Hypixel server, or in Hypixel lobby/minigame).
     */
    NOT_IN_SKYBLOCK,

    /**
     * Player is actively playing Hypixel SkyBlock.
     */
    IN_SKYBLOCK,

    /**
     * Player was in SkyBlock and temporarily lost the SkyBlock indicator (server hop, warp, island load).
     * Has a 10-second debounce window before transitioning to NOT_IN_SKYBLOCK.
     */
    WARP_GRACE_PERIOD
}