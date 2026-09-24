package cc.nexusdev.trails.api.animation;

import java.util.Map;
import java.util.UUID;

/** One render update. State belongs only to this player, trail kind, source and animation selection. */
public record AnimationFrame(UUID playerId, TrailKind kind, String source, AnimationPath path,
                             AnimationPoint playerPosition, double elapsedSeconds, long seed,
                             double spacing, int budget, AnimationStyle style, Map<String,Object> state) {
    public double phase() { double t=elapsedSeconds/style.periodSeconds(); return t-Math.floor(t); }
    /** Normalized path position, lateral offset and vertical offset in style-scaled blocks. */
    public AnimationPoint point(double progress, double lateral, double vertical) {
        return path.at(Math.clamp(progress,0,1)*path.length(), lateral*style.width()*style.scale(),
                vertical*style.height()*style.scale());
    }
}
