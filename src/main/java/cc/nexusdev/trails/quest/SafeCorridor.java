package cc.nexusdev.trails.quest;

import cc.nexusdev.trails.Route;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

/** Conservative loaded-block clearance checks for configured routes, not a pathfinder. */
final class SafeCorridor {
    private SafeCorridor() {}

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
