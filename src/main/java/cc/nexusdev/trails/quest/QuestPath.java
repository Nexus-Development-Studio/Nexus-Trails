package cc.nexusdev.trails.quest;

import cc.nexusdev.trails.Route;
import cc.nexusdev.trails.RouteGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Only joins recorded paths nearby, and only connects endpoints to nearby targets. */
public final class QuestPath {
    private QuestPath() {}

    public static Optional<RouteGeometry.Path> build(Route route, Route.Point player, Route.Point target,
                                                    double joinRadius, double targetRadius) {
        if (route.points().size() < 2 || route.points().getLast().distanceSquared(target) > targetRadius * targetRadius)
            return Optional.empty();
        double nearest = Double.POSITIVE_INFINITY;
        for (int i = 1; i < route.points().size(); i++) {
            Route.Point a = route.points().get(i - 1), b = route.points().get(i);
            nearest = Math.min(nearest, player.distanceSquared(a.interpolate(b, RouteGeometry.projection(player, a, b))));
        }
        if (nearest > joinRadius * joinRadius) return Optional.empty();
        List<Route.Point> joined = RouteGeometry.join(player, route.points());
        List<Route.Point> points = new ArrayList<>(joined);
        if (points.getLast().distanceSquared(target) > 1e-8) points.add(target);
        return Optional.of(new RouteGeometry.Path(points));
    }
}
