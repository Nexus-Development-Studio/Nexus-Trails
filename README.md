# Nexus Trails

Standalone personal particle navigation, extracted from NexusRegionManager's trail feature.

**Target platform:** Paper 1.21.11 or Folia 1.21.11, Java 21+. Standalone trails support both using the player's entity scheduler. The optional quest integration requires **Paper** and is disabled on Folia. Spigot is not supported. No NexusRegionManager, WorldGuard, WorldEdit, zMenu, economy plugin, or database is required.

## Install

1. Put `nexustrails-1.1.0.jar` in the server's `plugins` folder.
2. Start/restart the server. Settings appear in `plugins/NexusTrails/config.yml`.
3. As an operator, create a destination using one of the examples below.

The particle trail is visible only to the player following it. It blends a moving TRAIL core, color-transition dust and a soft dust glow, with a red-to-green progression toward the destination.

## Quick waypoint

Stand at the destination and run:

```text
/trail set spawn
```

Walk away and run `/trail spawn`. This makes a direct guide to the waypoint. **It does not calculate a walkable path or avoid walls.** Use a recorded route for roads, stairs, bridges and paths around obstacles.

## Record a route

Stand at the start of a road/path:

```text
/trail record market
```

Walk the route on foot, then run `/trail save` at the destination. Players can now use `/trail market` from the same world. Their trail joins the closest segment and follows the recorded direction to the endpoint. The connection from the player to that segment is a straight line, so players should start near the recorded road. Avoid self-intersecting routes where possible.

Use `/trail pause` and `/trail resume` for a break. Resume near the last recorded point. Teleporting, changing worlds, or flying interrupts recording; return to the route before resuming. `/trail cancel` discards only the unsaved recording. Unsaved recordings are discarded on disconnect/restart. Saved destinations survive restart.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/trail <id>` | Follow a destination | `nexustrails.use` |
| `/trail go <id>` | Explicit destination command | `nexustrails.use` |
| `/trail list` | List destinations | `nexustrails.use` |
| `/trail stop` | Stop your active trail | `nexustrails.use` |
| `/trail help` | Show commands | `nexustrails.use` |
| `/trail set <id>` | Save your location as a direct waypoint | `nexustrails.admin` |
| `/trail record <id>` | Start recording a walking route | `nexustrails.admin` |
| `/trail pause` | Pause recording | `nexustrails.admin` |
| `/trail resume` | Resume recording near its last point | `nexustrails.admin` |
| `/trail save` | Save the recording with your location as endpoint | `nexustrails.admin` |
| `/trail cancel` | Discard the current recording | `nexustrails.admin` |
| `/trail delete <id> DELETE` | Confirm deletion of a saved destination | `nexustrails.admin` |
| `/trail reload` | Reload config/destinations and stop active trails | `nexustrails.admin` |

Players have `nexustrails.use` by default. Administrators/operators have `nexustrails.admin` by default. Destination IDs are 1–48 lowercase letters, digits, hyphens or underscores, beginning with a letter/digit. Command names are reserved. Creation does not overwrite existing IDs; delete explicitly before replacing one.

## Storage and settings

Destinations and route points are saved in `plugins/NexusTrails/destinations.yml`, using atomic file replacement where supported. Example:

```yaml
destinations:
  spawn:
    world: world
    points:
      - x: 0.5
        y: 65.0
        z: 0.5
```

Edit the file while the server is stopped, or run `/trail reload` after edits. Keep a backup when editing routes manually. Invalid route data fails loading instead of replacing the current in-memory routes.

The config controls particle spacing, look-ahead distance, height above recorded feet positions, update rate, particle budget, trail timeout, arrival radius, and recording limits. Defaults render up to 120 particles per update every 6 ticks, with a 20-block look-ahead and 180-second timeout. Values are bounded to prevent zero-spacing loops and excessive configured budgets.

Trails continue after teleporting and rejoin the original route from the player's new position. Moving to another world pauses particles until the player returns to the destination's world; the existing timeout still applies and is not reset by teleporting. Route recording still pauses on teleport to avoid recording jumps. Trails stop on arrival, timeout, death, disconnect, plugin disable or explicit cancellation. Reloading stops ordinary trails. Tracing never loads chunks or changes blocks. The plugin does not create destinations automatically from NRM plots; it runs independently and does not alter NRM's existing trail feature.

## Build and verification

```text
mvn clean verify
```

Unit tests cover route joining/projection, corner following, duplicate points, invalid data, storage round-trips, deletion, failed reloads, immutable snapshots and config bounds. Actual particle visuals, player permissions, route recording and Paper/Folia lifecycle behavior should also be checked on a live server.

## Quest guidance: Citizens and BeautyQuests

Nexus Trails exposes private quest trails through Java and console commands. Nothing is tied to a particular server, NPC name or quest chain. Install compatible Citizens and BeautyQuests builds on Paper to use both integrations. Fixed-location guidance does not require either plugin. Existing configs can add the `quest-trails` section shown below; missing settings use defaults.

### Configure roads first

1. At the start of the road, run `/trail record village-to-guide`.
2. Walk the intended route around walls and buildings to the NPC, then `/trail save`.
3. Map the Citizens NPC ID to that recorded route in `config.yml`:

```yaml
quest-trails:
  join-radius: 2.0
  target-radius: 3.0
  npc-routes:
    '11': [village-to-guide]
    '13': [guide-to-merchant]
  location-routes: [village-to-guide]
  restore-rules: []
```

Record every referenced route, then run `/trail reload`. These NPC IDs and route names are examples; replace them with your own. Multiple routes per NPC allow different starting areas. The shortest eligible route is selected. Routes are directional and must contain at least two points; `/trail set` waypoints are not sufficient for quest guidance.

Players must be within `join-radius` of the recorded road. The route endpoint must be within `target-radius` of the destination. Both radii are bounded to 0.5–8 blocks. There is no straight-line fallback if a route is missing or too far away. An NPC's live position is refreshed each render update; if it leaves the route endpoint's allowed radius, guidance pauses until it returns or a suitable route is configured.

The visible route and its connector are checked for player-sized collision clearance in loaded chunks. A blocked corridor hides the trail until clear. This uses configured routes, **not automatic pathfinding**: it does not discover detours, validate ground support/hazards, or guarantee traversal of jumps, tight stairs and special blocks. Record clear walking routes and check them in game. Conservative clearance checks can pause trails on tightly recorded stairs; rerecord with enough clearance. Routes never teleport players or spawn NPCs.

### Console commands

Run these as console rewards/actions in your quest plugin, without a leading slash:

```text
questtrail show <player> npc <npcId>
questtrail show <player> location <world> <x> <y> <z>
questtrail clear <player>
questtrail status <player>
```

Use an online player's exact name or UUID. Commands require `nexustrails.quest.admin` (operators and console by default). Normal players do not need that permission to receive guidance. Fixed locations select from `location-routes` using the same endpoint and join limits.

For BeautyQuests, choose **console** execution and use its player placeholder:

```text
# Previous quest completion action:
questtrail show {PLAYER} npc 11

# Next quest start action:
questtrail clear {PLAYER}

# That quest's completion action:
questtrail show {PLAYER} npc 13
```

Put the clear action on quest cancellation/reset paths where appropriate too. Only paste the command lines, not the comments. Command rewards are documented by the [BeautyQuests implementation](https://github.com/SkytAsul/BeautyQuests/blob/2.1.0/core/src/main/java/fr/skytasul/quests/rewards/CommandReward.java). No separate Java integration is needed for these triggers.

### Restore from quest progress on login

Quest assignments are cleared on disconnect and plugin shutdown. To reconstruct guidance after login or server restart, configure rules using **BeautyQuests quest IDs**, which are separate from Citizens NPC IDs:

```yaml
quest-trails:
  # Keep the route mappings and radius settings from above here as well.
  restore-rules:
    - {after-quest: 101, until-quest: 102, npc: 13}
    - {after-quest: 100, until-quest: 101, npc: 11}
```

The first matching rule wins: `after-quest` must be completed and `until-quest` must be neither active nor completed. Put later steps first. Replace all example IDs with your server's IDs. This handles guidance **between quests**; it does not infer arbitrary in-quest stages or party progress. For other quest systems or stage-specific logic, use the Java API when that system finishes loading the player's progress.

Restoration waits one second and retries unavailable progress for up to ten attempts. A newer show/clear command always takes precedence over pending restoration. Missing or incompatible BeautyQuests APIs leave guidance unassigned and produce a console warning. Empty rules disable restoration. Quest state is read only; Nexus Trails does not edit BeautyQuests data or persist old trail assignments. Configure both the live command actions and the login rules.

The bridge targets the [BeautyQuests 2.1.0 public API](https://github.com/SkytAsul/BeautyQuests/tree/2.1.0/api/src/main/java/fr/skytasul/quests/api), with a fallback for the 1.0.x account API. Reflection keeps both plugins optional. API shape compatibility and mocked lifecycle behavior are tested; installation-specific live compatibility still needs a server check.

### Java API

The `api` classifier JAR contains only the public interface. Run `mvn install` locally to make it available as a Maven dependency (it is not published to a public Maven repository):

```xml
<dependency>
  <groupId>cc.nexusdev</groupId>
  <artifactId>nexustrails</artifactId>
  <version>1.1.0</version>
  <classifier>api</classifier>
  <scope>provided</scope>
</dependency>
```

Add `depend: [NexusTrails]` to your plugin's `plugin.yml` (or `softdepend` with a missing-service check). Do not shade the interface into your plugin and do not install the API-only JAR on the server. Obtain the service during `onEnable`:

```java
import cc.nexusdev.trails.api.QuestTrailAPI;

QuestTrailAPI trails = getServer().getServicesManager().load(QuestTrailAPI.class);
if (trails == null) {
    getLogger().warning("Quest trails are unavailable on this server.");
    return;
}

trails.showToNpc(player.getUniqueId(), 11);
trails.showToLocation(player.getUniqueId(), destination);
boolean assigned = trails.hasTrail(player.getUniqueId());
trails.clear(player.getUniqueId());
```

One quest assignment exists per player; each show call replaces it. Calls made off the Paper server thread are queued, so `hasTrail` may not reflect a queued mutation immediately. Locations are copied and must contain finite coordinates and a world. Negative NPC IDs are rejected. Offline players are not assigned guidance; restore it after login. Using an API instance after plugin shutdown throws `IllegalStateException` for mutations.

`hasTrail` means an assignment exists, including when paused for missing/despawned NPCs, another world, a missing/blocked route, death or arrival. At arrival particles stop; the assignment remains until clear/disconnect, allowing a moving NPC to be followed again. Quest guidance does not use the ordinary trail timeout. Ordinary `/trail` sessions are independent; `questtrail clear` only clears quest guidance. Existing particles expire naturally according to the configured duration.

### Verification before deployment

`mvn clean verify` builds both the server JAR and API JAR. Tests cover route corners and moving endpoints, unsafe joins, collision coordinates/low ceilings/fences, assignment replacement and copying, missing NPC/world recovery, unloaded chunks, console permissions, disconnect/shutdown cleanup, and restoration races. On a test Paper server, verify particle appearance, Citizens movement/despawn, BeautyQuests console actions and login restoration using real quest IDs before deploying to players.
