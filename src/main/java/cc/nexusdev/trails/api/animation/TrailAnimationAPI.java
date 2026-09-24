package cc.nexusdev.trails.api.animation;

import java.util.Set;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

/** Load through Bukkit's ServicesManager. IDs are namespaced, e.g. myplugin:aurora. */
public interface TrailAnimationAPI {
    void register(Plugin owner, String id, TrailAnimation animation);
    /** Only the registering owner can unregister an animation. */
    boolean unregister(Plugin owner, String id);
    Set<String> animations();
    /** Selection applies to this player's ordinary and quest trails, including already running ones. */
    void select(UUID playerId, String animationId);
    void reset(UUID playerId);
    String selected(UUID playerId);
    /** Immutable configured profile (or common style if no profile is configured). */
    AnimationStyle style(String animationId);
    void setStyle(UUID playerId, AnimationStyle style);
    void resetStyle(UUID playerId);
}
