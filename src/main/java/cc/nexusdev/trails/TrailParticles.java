package cc.nexusdev.trails;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/** Shared private particle renderer for ordinary and quest trails. */
public final class TrailParticles {
    private TrailParticles() {}

    public static void spawn(Player player, TrailSettings settings, Route.Point point, Route.Point ahead, double ratio) {
        Color color = Color.fromRGB((int) Math.round(255 * (1 - ratio)), (int) Math.round(255 * ratio), 0);
        double y = point.y() + settings.height();
        Location target = new Location(player.getWorld(), ahead.x(), ahead.y() + settings.height(), ahead.z());
        player.spawnParticle(Particle.TRAIL, point.x(), y, point.z(), 4, .25, .15, .25, 0,
                new Particle.Trail(target, color, settings.duration()));
        player.spawnParticle(Particle.DUST_COLOR_TRANSITION, point.x(), y, point.z(), 1, .15, 0, .15, 0,
                new Particle.DustTransition(color, Color.BLUE, 1.4f));
        player.spawnParticle(Particle.DUST, point.x(), y, point.z(), 1, .08, 0, .08, 0,
                new Particle.DustOptions(color, 1.2f));
    }
}
