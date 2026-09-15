# Wise Old Block Companion

[![Build and Release](https://github.com/freausan/wob-companion/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/freausan/wob-companion/actions/workflows/build-and-release.yml)

A lightweight, production-ready Fabric 26.1.2 client-side mod for Minecraft that automatically detects player play sessions on **Hypixel SkyBlock** and reports session boundary events (`connect` and `disconnect`) to the **Wise Old Block** REST API (`POST /players/{playerUuid}`).

## Features

- **Automated SkyBlock Session Detection**:
  - Validates connection to `*.hypixel.net`.
  - Accurately checks the scoreboard sidebar objective display name (`SKYBLOCK`) and score lines.
  - Automatically sends `{"event": "connect"}` upon entering SkyBlock.
  - Automatically sends `{"event": "disconnect"}` upon leaving SkyBlock or stopping the game.
- **Warping & Server-Hopping Grace Period**:
  - Warping between SkyBlock servers (`/warp hub`, `/is`, `/warp dungeon`) will **not** trigger premature disconnect/connect events.
  - Features a **10-second debounce grace period** before concluding that the player has truly departed SkyBlock.
  - If more than **30 minutes** have elapsed since the last update, warping will automatically request a mid-session stat snapshot update.
- **Asynchronous & Non-Blocking**:
  - All network traffic is handled by Java's native `HttpClient` on a dedicated background worker daemon.
  - Strict 5-second request timeout to guarantee zero client freeze or tick-loop stutter.
  - Synchronous bounded flush on `ClientLifecycleEvents.CLIENT_STOPPING` so session end events are not dropped during game exit.
- **In-Game Chat Feedback**:
  - `§b[Wise Old Block] §7SkyBlock session started.`
  - `§b[Wise Old Block] §7SkyBlock session ended.`
  - `§b[Wise Old Block] §7Profile updated recently (cooldown active).` (on HTTP 429)
  - `§b[Wise Old Block] §7Mid-session stats update requested.`
- **Client Commands**:
  - `/wob status` - Show current tracking state, backend URL, and active player UUID
  - `/wob update` - Manually request an immediate snapshot update
  - `/wob toggle` - Enable or disable session reporting
  - `/wob notify` - Toggle in-game chat messages on or off
  - `/wob help` - List available commands (also accessible via `/companion`)

## Configuration

The configuration file is saved at `config/wise-old-block.json`:

```json
{
  "backendUrl": "https://api.wiseoldblock.xyz",
  "enabled": true,
  "notifyInChat": true
}
```

- **`backendUrl`**: Base URL of the Wise Old Block API backend (default: `https://api.wiseoldblock.xyz`, also accepts local dev instances e.g. `http://localhost:3001`).
- **`enabled`**: Master toggle for session boundary tracking.
- **`notifyInChat`**: Whether to display clean status messages in client chat.

## Building from Source

Requires **Java 25**:

```bash
./gradlew build
```

Built JAR files are located in `build/libs/`.

## License

[MIT](LICENSE)