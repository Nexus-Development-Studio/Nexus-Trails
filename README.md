# Nexus Trails

Standalone personal particle navigation, extracted from NexusRegionManager's trail feature.

**Target platform:** Paper 1.21.11 or Folia 1.21.11, Java 21+. One JAR supports both using the player's entity scheduler. Spigot is not supported. No NexusRegionManager, WorldGuard, WorldEdit, zMenu, economy plugin, or database is required.

## Install

1. Put `nexustrails-1.0.0.jar` in the server's `plugins` folder.
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

Trails stop on arrival, timeout, teleport, world change, death, disconnect, plugin disable or explicit cancellation. Reloading also stops active trails. Tracing never loads chunks or changes blocks. The plugin does not create destinations automatically from NRM plots; it runs independently and does not alter NRM's existing trail feature.

## Build and verification

```text
mvn clean verify
```

Unit tests cover route joining/projection, corner following, duplicate points, invalid data, storage round-trips, deletion, failed reloads, immutable snapshots and config bounds. Actual particle visuals, player permissions, route recording and Paper/Folia lifecycle behavior should also be checked on a live server.
