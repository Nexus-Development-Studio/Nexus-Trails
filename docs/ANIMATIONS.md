# Trail animations

Nexus Trails 1.3.1 includes all 46 built-in effects below. Colors, particle type, period, size, geometry scale and effect parameters come from `plugins/NexusTrails/animations.yml`. The built-ins are registered through the same registry used by extensions; custom plugins are not limited to the built-in names or geometries.

## Server configuration and commands

```yaml
default-animation: comet
fallback-animation: breathing
style:
  particle: DUST
  palette: ['#FFD166', '#F4F1DE', '#80CED7']
  period-seconds: 3.0
  scale: 1.0
  width: 0.8
  height: 1.0
  brightness: 1.0
  base-brightness: 0.08
  size: 0.8
  duration-ticks: 12
  parameters:
    markers: 5
    frequency: 3
    pulse-width: 0.12
    tail-length: 0.3
    ring-points: 12
    origin: 0.0
    wait-distance: 3.0
    look-ahead: 5.0
patterns:
  comet:
    palette: ['#FF8800', '#FFF0B3']
    period-seconds: 2.5
    parameters: {tail-length: 0.4}
  clockwork-moth:
    particle: DUST_COLOR_TRANSITION
    parameters: {wait-distance: 3.0, look-ahead: 6.0}
  'myplugin:aurora':
    palette: ['#B8F2E6', '#A9DEF9']
```

The shipped file lists every built-in profile. Profile values inherit from `style`; profiles can override any of those fields. Keep the full style section, or rely on shipped defaults for omitted fields. `fallback-animation` must name a built-in. `default-animation` can name a custom animation once its plugin registers it; the fallback is used while it is unavailable. Reload both configuration files with `/trail reload`. An invalid animation reload reports an error and preserves the previous animation configuration.

```text
trailanimation list
trailanimation set <player> clockwork-moth
trailanimation set <player> nexustrails:orbiting-guide
trailanimation set <player> myplugin:aurora
trailanimation reset <player>
```

Commands require `nexustrails.animation.admin` (console/operators by default). Use the online player's exact name or UUID. Selection takes effect on the next render update and applies to that player's ordinary and quest trails. Selection does not start a destination by itself. Selections and programmatic style overrides are saved by player UUID in `plugins/NexusTrails/animation-preferences.yml` and survive logout, configuration reload and server restart. Restored quest trails use the saved choice. Only an explicit reset removes a preference; resetting selection does not reset a programmatic style override.

Changes are saved immediately. A failed save reports an error and keeps the previous preference. Back up `animation-preferences.yml` with your other plugin data; it is managed by the plugin, while `animations.yml` defines effects and defaults. A custom animation whose provider is unavailable temporarily uses the fallback without replacing the saved choice, and resumes when its provider registers again. Frame-local listener overrides are not saved. Choices already discarded by version 1.3.0 must be selected once after upgrading.

BeautyQuests can run these console actions consecutively:

```text
trailanimation set {PLAYER} clockwork-moth
questtrail show {PLAYER} npc 11
```

Particle types `DUST`, `DUST_COLOR_TRANSITION` and `TRAIL` support the configurable palette. Bukkit particles that require no data, such as `FLAME`, are also supported; their colors are determined by Minecraft, so brightness is approximated by emission density. Other data-requiring particles are available to custom animations through explicit particle/data emission. `duration-ticks` applies to `TRAIL`; dust and most other particle lifetimes are controlled by the client. Trail brightness is simulated through re-emission, not by editing particles already sent to the client.

`render.update-interval-ticks`, `render.particle-spacing`, `render.max-particles-per-update`, `render.max-ahead` and `render.height-offset` still come from `config.yml`. Lower update intervals make fast effects smoother at a higher rendering cost. Large effects are reduced by the per-frame particle budget. Width/height/scale affect decorative geometry around the already validated route; those decorative offsets are not themselves navigation or collision checks.

## Built-in catalog

All IDs accept the `nexustrails:` prefix. Short IDs below are aliases for that namespace.

| ID | Effect |
| --- | --- |
| `breathing` | The trail brightens and fades together. |
| `chase` | A bright band travels forward. |
| `comet` | A moving head leaves a fading tail. |
| `wave` | Smooth brightness waves flow forward. |
| `ripple` | A pulse expands from a configurable origin. |
| `heartbeat` | Two quick pulses followed by a longer fade. |
| `twinkle` | Deterministic, staggered gentle flickers. |
| `sparkle` | Brief bright flashes above the dim baseline. |
| `scan` | A band moves forward and backward. |
| `sequential-fill` | The route fills progressively, then fades together. |
| `dripping` | Spaced clusters flow forward. |
| `theater-chase` | Every third sample lights up in a three-step sequence. |
| `stacking` | Forward pulses accumulate at the end, then fade. |
| `crossfade-chase` | Two moving bands crossfade continuously. |
| `tick-tock` | Advancing marks alternate sides. |
| `clock-hands` | Rings carry rotating hands along the route. |
| `time-skip` | A pulse freezes, disappears and reappears ahead with an afterimage. |
| `reverse-echo` | A forward pulse has faint backward echoes. |
| `clockwork-footsteps` | Alternating world-space footprints appear ahead and dissolve in place. |
| `unwinding-spring` | A coil stretches forward and reforms. |
| `steam-bursts` | Sequential curling jets rise from the path. |
| `fuse-burn` | A bright forward tip leaves fading sparks. |
| `orbiting-guide` | Two streams spiral around the centerline. |
| `shattered-seconds` | Fragments assemble into an arrow and scatter. |
| `pendulum` | A forward cluster swings across the trail. |
| `ink-reveal` | A spreading front soaks outward and evaporates. |
| `mechanical-relay` | Sequential clusters wake with expanding bursts. |
| `ghost-of-tomorrow` | A particle silhouette dissolves and reforms farther ahead. |
| `escaping-seconds` | Sparks leave the viewer, pause, then accelerate toward the route. |
| `clockwork-moth` | A fluttering guide waits in world space until the viewer approaches. |
| `broken-timeline` | Offset fragments slide together and separate. |
| `pocket-watch-swing` | A glowing watch ring swings on a particle chain. |
| `borrowed-footsteps` | Invisible-walker footprints sometimes vanish and redraw. |
| `mechanical-fireflies` | Lights gather, drift and spread as a swarm. |
| `stitching-time` | A zigzag needle leaves fading stitches. |
| `falling-hourglass` | Grains fall into funnels and spill forward. |
| `echo-doors` | Door frames open outward and dissolve. |
| `magnetic-shavings` | Flecks tremble, align into directional marks and scatter. |
| `clock-teeth` | Rising bars roll forward like gear teeth. |
| `rewinding-ribbon` | A ribbon coils backward and unwinds forward. |
| `future-glimpse` | A far arrow appears first; markers then connect backward toward the viewer. |
| `pressure-leak` | A traveling bulge releases curling puffs at bends. |
| `orbit-collapse` | A ring collapses and shoots forward to the next orbit. |
| `unfinished-blueprint` | Dotted construction lines appear before the arrow, then fade. |
| `delayed-shadow` | Stationary steps of a guide catch up after a delay. |
| `second-hand-sweep` | A beam rotates at a bend and illuminates the path it sweeps across. |

The end of the currently visible walking section is the animation's forward direction. Long routes are still shown through the look-ahead window, and elevator gaps are never treated as continuous walking paths. No animations teleport players or spawn real NPC/entity silhouettes.

## API selection

Use the `nexustrails` **1.3.1** `api` classifier with Maven `provided` scope, and `depend: [NexusTrails]` (or `softdepend` plus a missing-service check). Do not shade the API or install the API-only JAR as a server plugin. The API includes all animation interfaces and event classes without implementation classes.

```java
import cc.nexusdev.trails.api.animation.TrailAnimationAPI;
import cc.nexusdev.trails.api.QuestTrailAPI;

TrailAnimationAPI animations = getServer().getServicesManager().load(TrailAnimationAPI.class);
QuestTrailAPI quests = getServer().getServicesManager().load(QuestTrailAPI.class);
if (animations == null) return;

animations.select(player.getUniqueId(), "nexustrails:clockwork-moth");
if (quests != null) quests.showToNpc(player.getUniqueId(), 11);

// Later, switch an already-running trail:
animations.select(player.getUniqueId(), "nexustrails:comet");
animations.reset(player.getUniqueId());
```

`animations.animations()` returns registered IDs, including third-party IDs. Registration and preference access are thread-safe. Preference mutations serialize disk writes before publishing an immutable snapshot and throw `IllegalStateException` if saving fails. Set persistent preferences when they change, rather than on every animation frame; use the selection event for frame-local changes. Rendering itself always stays on the ordinary trail's owning entity scheduler, or the quest service's Paper server thread. The animation service is available for ordinary trails on Folia; the Citizens/BeautyQuests quest service still requires Paper.

For programmatic appearance, pass an immutable `AnimationStyle` to `setStyle(UUID, style)`. Read a configured profile with `animations.style("comet")`, then use helpers `withPalette`, `withParticle` and `withPeriod` to derive a style (or use the full constructor). For example: `animations.setStyle(playerId, animations.style("comet").withPeriod(5.0))`. `resetStyle(UUID)` returns to configuration. Per-player styles override profile configuration; listener styles override both for that frame.

## Listener selection

```java
import cc.nexusdev.trails.api.animation.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuestEffects implements Listener {
    @EventHandler
    public void chooseAnimation(TrailAnimationSelectEvent event) {
        if (event.getKind() == TrailKind.QUEST && event.getSource().equals("npc:11")) {
            event.setAnimationId("nexustrails:echo-doors");
        }
        // Optional frame-local appearance override:
        // event.setStyle(event.getStyle().withPalette(List.of(Color.AQUA, Color.WHITE)));
        // event.setCancelled(true); // suppress only this frame
    }
}
```

Register the listener with Bukkit normally. The event runs before **every frame**, so keep it quick and avoid disk/network work. Source IDs are saved destination IDs for ordinary trails, `npc:<id>` for NPC quests, and `location:<world>:<x>:<y>:<z>` for location quests. Changing only the animation ID selects that profile's style automatically; explicitly setting a style takes precedence. Unknown listener-selected IDs fall back to the configured fallback.

On Folia the event is marked asynchronous and fires on the viewer's owning entity thread. Access only that player's region and thread-safe data. It is not an invitation to touch other players or arbitrary regions. Paper events use the main thread. Listener selection is frame-local and does not change the persistent per-player API selection.

## Register a new animation

```java
import cc.nexusdev.trails.api.animation.*;

TrailAnimationAPI animations = getServer().getServicesManager().load(TrailAnimationAPI.class);
if (animations == null) return;

animations.register(this, "myplugin:aurora", (frame, particles) -> {
    double phase = frame.phase();
    for (double u = 0; u <= 1 && particles.remaining() > 0; u += 0.04) {
        double side = Math.sin((u * 3 - phase) * Math.PI * 2) * 0.5;
        AnimationPoint position = frame.point(u, side, 0.4);
        // Configured particle, palette and brightness; always private to the viewer.
        particles.emit(position, 0.8, u);
    }
});
```

`frame.path()` is an immutable snapshot of the current visible section. `frame.point(u, lateral, vertical)` uses normalized progress and style-scaled local offsets. `path.at(distance)` uses distances in blocks. `frame.playerPosition()` is a snapshot, not a live Bukkit entity. `elapsedSeconds`, `seed`, `spacing`, `budget` and `style` are available. `frame.state()` is scratch storage isolated by player, trail kind, source and selected animation; it is reset on selection changes, teleport, trail cleanup and configuration reload. Use it for waiting guides or particle histories. Do not store unbounded histories.

For a custom Bukkit particle with explicit data:

```java
particles.emit(Particle.FLAME, frame.point(0.5, 0, 0.2), 1, null);
particles.emit(Particle.DUST, frame.point(0.6, 0, 0.2), 1,
    new Particle.DustOptions(frame.style().color(0.6, 1), frame.style().size()));
```

The emitter validates particle data types and counts, caps output to the remaining budget, and sends only to the assigned viewer. It is valid only during the render callback and only on that callback's thread. Retaining it or emitting asynchronously throws an exception. Positive counts are required; zero-count directional emission is not exposed. For custom movement, update positions using the frame clock.

Use a unique namespace such as `myplugin:aurora`; `nexustrails:` is reserved. IDs are case-insensitive and underscores normalize to hyphens. Duplicate registrations are rejected. Only the registering plugin may unregister its animation with `unregister(owner, id)`. Registrations are automatically removed when that plugin disables. Render exceptions are isolated to that animation/player state and switch subsequent frames to the fallback, without repeatedly invoking the broken callback. Plugin callbacks must still finish promptly: particle budgets cannot stop arbitrary slow plugin code.

## Preview and verification

Run `mvn clean verify`, then open `target/animation-preview.html` to play all 46 effects, filter by name and pause/restart. `target/animation-atlas.png` provides a static contact sheet. These use emitted coordinates and configured colors captured from the real Java callbacks. They do not simulate Minecraft textures, lingering particles or client performance. Confirm final appearance in game with the chosen particle type and update interval.
