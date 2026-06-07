# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build          # compile + produce remapped jar in build/libs/
./gradlew runClient      # launch Minecraft client with mod loaded
./gradlew runServer      # launch dedicated server with mod loaded
```

No test suite — `./gradlew test` is a no-op stub.

Version bumps go in `gradle.properties` (`mod_version`, `minecraft_version`, `yarn_mappings`, `fabric_api_version`). `fabric.mod.json` version is injected from `mod_version` at build time via `processResources`.

## Architecture

Fabric mod targeting MC 1.21.11. Dual entrypoint: server-side logic + client-side HUD. No mixins in active use (`ExampleMixin` is an empty stub).

**Data flow:**

1. `CreeperAlarmServer` (server, `ModInitializer`) — runs every world tick. For each online player, queries a 32-block radius for `CreeperEntity` whose `getTarget()` is that player. Sends `CreeperAlarmPayload` S2C only when state changes (tracked in `previousState` map keyed by UUID).

2. `CreeperAlarmPayload` — thin `CustomPayload` record. Single boolean field `creeperTargeting`. Identifier: `creeper-alarm:alert`.

3. `CreeperAlarm` (client, `ClientModInitializer`) — receives payload, manages fade state (`overlayAlpha` 0→1 at `FADE_SPEED` per tick), plays `creeper-alarm:alarm` sound on leading edge with 5s cooldown. Renders `textures/hud/warning.png` full-screen at variable alpha via `HudRenderCallback` using `RenderPipelines.GUI_TEXTURED`.

**Key constraint:** Must be installed server-side in multiplayer — the detection logic runs entirely on the server. Works with Sinytra Connector (Forge compat layer).