package cc.nexusdev.trails.quest;

import cc.nexusdev.trails.Route;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import java.util.*;

/** Conservative loaded-block clearance checks for configured routes, not a pathfinder. */
final class SafeCorridor {
    private SafeCorridor() {}

    /** Lift interpolated stair/slab samples onto the step, without lifting through full walls. */
    static Route.Point adjust(World world, Route.Point point) {
        if (clear(world, point)) return point;
        for (double y : surfaces(world, point, 1.05, 0)) {
            Route.Point lifted = new Route.Point(point.x(), y, point.z());
            if (clear(world, lifted)) return lifted;
        }
        return null;
    }

    /** A supported, player-sized walking position for the connector search. */
    static Route.Point stand(World world, Route.Point point, double rise, double drop) {
        for (double y : surfaces(world, point, rise, drop)) {
            Route.Point feet = new Route.Point(point.x(), y, point.z());
            if (clear(world, feet)) return feet;
        }
        return null;
    }

    private static List<Double> surfaces(World world, Route.Point point, double rise, double drop) {
        if (Math.abs(point.x()) > 30_000_000 || Math.abs(point.z()) > 30_000_000
                || point.y() < world.getMinHeight() || point.y() > world.getMaxHeight()) return List.of();
        Set<Double> heights = new HashSet<>();
        for (int x = (int) Math.floor(point.x() - .3); x <= (int) Math.floor(point.x() + .3); x++) {
            for (int z = (int) Math.floor(point.z() - .3); z <= (int) Math.floor(point.z() + .3); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return List.of();
                for (int y = Math.max(world.getMinHeight(), (int) Math.floor(point.y() - drop) - 1);
                     y <= Math.min(world.getMaxHeight() - 1, (int) Math.floor(point.y() + rise)); y++) {
                    for (BoundingBox box : world.getBlockAt(x, y, z).getCollisionShape().getBoundingBoxes()) {
                        double top = y + box.getMaxY();
                        if (top >= point.y() - drop - .01 && top <= point.y() + rise + .01
                                && x + box.getMaxX() > point.x() - .3 && x + box.getMinX() < point.x() + .3
                                && z + box.getMaxZ() > point.z() - .3 && z + box.getMinZ() < point.z() + .3) heights.add(top);
                    }
                }
            }
        }
        return heights.stream().sorted(Comparator.comparingDouble(y -> Math.abs(y - point.y()))).toList();
    }

    static boolean clear(World world, Route.Point point) {
        if (Math.abs(point.x()) > 30_000_000 || Math.abs(point.z()) > 30_000_000
                || point.y() < world.getMinHeight() || point.y() + 1.8 >= world.getMaxHeight()) return false;
        BoundingBox body = new BoundingBox(point.x() - .3, point.y() + .05, point.z() - .3,
                point.x() + .3, point.y() + 1.8, point.z() + .3);
        for (int x = (int) Math.floor(body.getMinX()); x <= (int) Math.floor(body.getMaxX()); x++) {
            for (int z = (int) Math.floor(body.getMinZ()); z <= (int) Math.floor(body.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
                // Include the block below: fences and walls can extend above their own block.
                for (int y = Math.max(world.getMinHeight(), (int) Math.floor(body.getMinY()) - 1);
                     y <= (int) Math.floor(body.getMaxY()); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    // Collision-shape boxes are block-local. Include the player's width and height.
                    for (BoundingBox box : block.getCollisionShape().getBoundingBoxes()) {
                        if (body.overlaps(box.clone().shift(x, y, z))) return false;
                    }
                }
            }
        }
        return true;
    }
}
