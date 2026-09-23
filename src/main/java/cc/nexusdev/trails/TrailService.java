package cc.nexusdev.trails;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Personal forward-moving TRAIL particles, color-transition accents, and dust glow extracted from NRM. */
public final class TrailService {
    private final Plugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    public TrailService(Plugin plugin) { this.plugin = plugin; }

    public void start(Player player, Route destination, TrailSettings settings) {
        if (!player.getWorld().getName().equals(destination.world())) {
            player.sendMessage("§cThis destination is in world §f" + destination.world() + "§c. Travel there first.");
            return;
        }
        Route.Point from = point(player.getLocation());
        if (from.distanceSquared(destination.points().getLast()) <= settings.arrivalRadius() * settings.arrivalRadius()) {
            stop(player.getUniqueId());
            player.sendMessage("§aYou are already at " + destination.id() + ".");
            return;
        }
        stop(player.getUniqueId());
        Session session = new Session(player, destination, settings,
                new RouteGeometry.Path(RouteGeometry.join(from, destination.points())));
        sessions.put(player.getUniqueId(), session);
        session.task = player.getScheduler().runAtFixedRate(plugin, task -> session.tick(), session::retired, 1L, settings.interval());
        if (session.task == null) { session.retired(); return; }
        player.sendMessage("§aFollow the trail to §f" + destination.id() + "§a. §7/trail stop to cancel.");
    }

    public boolean stop(UUID id) {
        Session session = sessions.remove(id);
        if (session == null) return false;
        session.cancel();
        return true;
    }
    public void stopDestination(String id) {
        sessions.values().stream().filter(s -> s.destination.id().equals(id)).forEach(s -> stop(s.player.getUniqueId()));
    }
    public void shutdown() { for (UUID id : java.util.List.copyOf(sessions.keySet())) stop(id); }
    public static Route.Point point(Location l) { return new Route.Point(l.getX(), l.getY(), l.getZ()); }

    private final class Session {
        final Player player;
        final Route destination;
        final TrailSettings settings;
        final RouteGeometry.Path path;
        final long started = System.nanoTime();
        volatile ScheduledTask task;
        Session(Player player, Route destination, TrailSettings settings, RouteGeometry.Path path) {
            this.player = player; this.destination = destination; this.settings = settings; this.path = path;
        }
        void cancel() { ScheduledTask current = task; if (current != null) current.cancel(); }
        void retired() { sessions.remove(player.getUniqueId(), this); }
        void finish(String message) {
            if (sessions.remove(player.getUniqueId(), this)) {
                cancel();
                if (message != null && player.isOnline()) player.sendMessage(message);
            }
        }
        void tick() {
            if (sessions.get(player.getUniqueId()) != this) { cancel(); return; }
            if (!player.isOnline() || !player.getWorld().getName().equals(destination.world()) || player.isDead()) {
                finish(null); return;
            }
            if ((System.nanoTime() - started) / 1_000_000_000L >= settings.timeoutSeconds()) {
                finish("§eTrail timed out. Use /trail " + destination.id() + " to restart."); return;
            }
            Route.Point position = point(player.getLocation());
            if (position.distanceSquared(path.end()) <= settings.arrivalRadius() * settings.arrivalRadius()) {
                finish("§aDestination reached: §f" + destination.id()); return;
            }
            double progress = path.progress(position);
            double end = Math.min(path.length(), progress + settings.maxAhead());
            double start = Math.min(end, progress + settings.minAhead());
            int spawned = 0;
            for (double distance = start; distance <= end && spawned + 6 <= settings.budget(); distance += settings.spacing()) {
                Route.Point p = path.at(distance), ahead = path.at(Math.min(path.length(), distance + .8));
                double ratio = path.length() <= .001 ? 1 : distance / path.length();
                Color color = Color.fromRGB((int) Math.round(255 * (1 - ratio)), (int) Math.round(255 * ratio), 0);
                double y = p.y() + settings.height();
                Location target = new Location(player.getWorld(), ahead.x(), ahead.y() + settings.height(), ahead.z());
                player.spawnParticle(Particle.TRAIL, p.x(), y, p.z(), 4, .25, .15, .25, 0,
                        new Particle.Trail(target, color, settings.duration()));
                player.spawnParticle(Particle.DUST_COLOR_TRANSITION, p.x(), y, p.z(), 1, .15, 0, .15, 0,
                        new Particle.DustTransition(color, Color.BLUE, 1.4f));
                player.spawnParticle(Particle.DUST, p.x(), y, p.z(), 1, .08, 0, .08, 0,
                        new Particle.DustOptions(color, 1.2f));
                spawned += 6;
            }
        }
    }
}
