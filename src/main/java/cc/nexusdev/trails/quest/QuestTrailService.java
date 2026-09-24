package cc.nexusdev.trails.quest;

import cc.nexusdev.trails.*;
import cc.nexusdev.trails.api.QuestTrailAPI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

/** Paper server-thread implementation. Quest assignments are rebuilt from progress on login. */
public final class QuestTrailService implements QuestTrailAPI, Listener {
    private final JavaPlugin plugin;
    private final RouteStore store;
    private final Supplier<TrailSettings> settings;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Object> restores = new HashMap<>();
    private final Set<BukkitTask> restoreTasks = new HashSet<>();
    private final BukkitTask ticker;
    private volatile boolean closed;
    private Config config;

    public QuestTrailService(JavaPlugin plugin, RouteStore store, Supplier<TrailSettings> settings) {
        this.plugin = plugin;
        this.store = store;
        this.settings = settings;
        reload();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void reload() { config = Config.read(plugin.getConfig()); }

    @Override public void showToNpc(UUID playerId, int npcId) {
        Objects.requireNonNull(playerId, "playerId");
        if (npcId < 0) throw new IllegalArgumentException("NPC ID must be nonnegative");
        submit(() -> assign(playerId, new Session(npcId, null)));
    }

    @Override public void showToLocation(UUID playerId, Location destination) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(destination, "destination");
        if (destination.getWorld() == null) throw new IllegalArgumentException("Destination needs a loaded world");
        Location copy = destination.clone();
        TrailService.point(copy); // validates finite coordinates before queuing
        submit(() -> assign(playerId, new Session(null, copy)));
    }

    private void assign(UUID id, Session session) {
        restores.remove(id); // a new command/API decision always wins over delayed login restoration
        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) sessions.put(id, session);
        else sessions.remove(id);
    }

    @Override public void clear(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        submit(() -> { restores.remove(playerId); sessions.remove(playerId); });
    }

    @Override public boolean hasTrail(UUID playerId) { return sessions.containsKey(Objects.requireNonNull(playerId)); }

    private void submit(Runnable work) {
        if (closed) throw new IllegalStateException("Nexus Trails is disabled");
        if (Bukkit.isPrimaryThread()) work.run();
        else Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) work.run(); });
    }

    private void tick() {
        TrailSettings render = settings.get();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || !player.isOnline()) { sessions.remove(entry.getKey(), session); continue; }
            if (++session.ticks % render.interval() != 0) continue;
            try { render(player, session, render); }
            catch (ReflectiveOperationException | IllegalStateException ex) {
                notice(player, session, "The quest NPC is unavailable. Guidance will resume when it returns.");
                if (!session.warned) { plugin.getLogger().warning("Quest trail target unavailable: " + ex.getMessage()); session.warned = true; }
            }
        }
    }

    private void render(Player player, Session session, TrailSettings render) throws ReflectiveOperationException {
        if (player.isDead()) return;
        Location target = session.npcId == null ? session.destination : CitizensTarget.location(session.npcId);
        if (target == null) { notice(player, session, "The quest NPC is not spawned. Guidance is paused."); return; }
        if (!player.getWorld().equals(target.getWorld())) {
            notice(player, session, "Your quest destination is in " + target.getWorld().getName() + ". Travel there to resume guidance.");
            return;
        }
        Route.Point position = TrailService.point(player.getLocation()), endpoint = TrailService.point(target);
        if (position.distanceSquared(endpoint) <= render.arrivalRadius() * render.arrivalRadius()) {
            notice(player, session, "Quest destination reached."); return;
        }
        List<String> ids = session.npcId == null ? config.locationRoutes : config.npcRoutes.getOrDefault(session.npcId, List.of());
        RouteGeometry.Path path = null;
        for (String id : ids) {
            Route route = store.get(id);
            if (route == null || !route.world().equals(target.getWorld().getName())) continue;
            var candidate = QuestPath.build(route, position, endpoint, config.joinRadius, config.targetRadius);
            if (candidate.isPresent() && (path == null || candidate.get().length() < path.length())) path = candidate.get();
        }
        if (path == null) {
            notice(player, session, "No nearby configured quest route. Move closer to the marked road, or ask an administrator to configure it.");
            return;
        }
        double end = Math.min(path.length(), render.maxAhead());
        // Check the entire visible corridor, including the player-to-route connector and every corner.
        for (double d = 0; d < end; d += .2) {
            if (!SafeCorridor.clear(player.getWorld(), path.at(d))) {
                notice(player, session, "Quest route blocked or not loaded. Guidance is paused."); return;
            }
        }
        if (!SafeCorridor.clear(player.getWorld(), path.at(end))) {
            notice(player, session, "Quest route blocked or not loaded. Guidance is paused."); return;
        }
        session.notice = null;
        session.warned = false;
        int spawned = 0;
        for (double d = Math.min(end, render.minAhead()); d <= end && spawned + 6 <= render.budget(); d += render.spacing()) {
            TrailParticles.spawn(player, render, path.at(d), path.at(Math.min(path.length(), d + .8)),
                    path.length() <= .001 ? 1 : d / path.length());
            spawned += 6;
        }
    }

    private static void notice(Player player, Session session, String message) {
        if (!message.equals(session.notice)) { player.sendMessage("§e" + message); session.notice = message; }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { restore(event.getPlayer()); }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sessions.remove(id);
        restores.remove(id);
    }

    public void restore(Player player) {
        if (config.restoreRules.isEmpty()) return;
        Object token = new Object();
        restores.put(player.getUniqueId(), token);
        restoreLater(player.getUniqueId(), token, 0);
    }

    private void restoreLater(UUID id, Object token, int attempt) {
        BukkitTask task = new BukkitRunnable() { @Override public void run() {
            restoreTasks.removeIf(scheduled -> scheduled.getTaskId() == getTaskId());
            if (closed || restores.get(id) != token) return;
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) { restores.remove(id); return; }
            try {
                Integer npc = BeautyQuestsProgress.destination(player, config.restoreRules);
                restores.remove(id);
                // Current quest progress is authoritative; do not persist and replay stale assignments.
                sessions.remove(id);
                if (npc != null) assign(id, new Session(npc, null));
            } catch (ReflectiveOperationException | IllegalStateException ex) {
                if (attempt < 9) restoreLater(id, token, attempt + 1);
                else {
                    restores.remove(id);
                    plugin.getLogger().warning("Could not restore quest guidance for " + player.getName() + ": " + ex.getMessage());
                }
            }
        } }.runTaskLater(plugin, 20);
        restoreTasks.add(task);
    }

    public void shutdown() {
        closed = true;
        ticker.cancel();
        restoreTasks.forEach(BukkitTask::cancel);
        restoreTasks.clear();
        restores.clear();
        sessions.clear();
    }

    private static final class Session {
        final Integer npcId;
        final Location destination;
        int ticks;
        String notice;
        boolean warned;
        Session(Integer npcId, Location destination) { this.npcId = npcId; this.destination = destination; }
    }

    private record Config(Map<Integer, List<String>> npcRoutes, List<String> locationRoutes,
                          List<Map<?, ?>> restoreRules, double joinRadius, double targetRadius) {
        static Config read(ConfigurationSection root) {
            Map<Integer, List<String>> mappings = new HashMap<>();
            ConfigurationSection section = root.getConfigurationSection("quest-trails.npc-routes");
            if (section != null) for (String key : section.getKeys(false)) {
                int id = Integer.parseInt(key);
                if (id < 0) throw new IllegalArgumentException("NPC IDs must be nonnegative");
                mappings.put(id, List.copyOf(section.getStringList(key)));
            }
            List<Map<?, ?>> rules = root.getMapList("quest-trails.restore-rules");
            for (Map<?, ?> rule : rules) for (String key : List.of("after-quest", "until-quest", "npc"))
                BeautyQuestsProgress.integer(rule, key);
            return new Config(Map.copyOf(mappings), List.copyOf(root.getStringList("quest-trails.location-routes")),
                    List.copyOf(rules), radius(root, "join-radius", 2), radius(root, "target-radius", 3));
        }
        private static double radius(ConfigurationSection root, String key, double fallback) {
            double value = root.getDouble("quest-trails." + key, fallback);
            return Double.isFinite(value) ? Math.clamp(value, .5, 8) : fallback;
        }
    }
}
