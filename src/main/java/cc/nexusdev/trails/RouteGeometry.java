package cc.nexusdev.trails;

import java.util.ArrayList;
import java.util.List;
import cc.nexusdev.trails.Route.Point;

/** Pure route geometry; never loads chunks or performs automatic obstacle pathfinding. */
public final class RouteGeometry {
    private RouteGeometry() {}

    public static double projection(Point p, Point a, Point b) {
        double lengthSquared = a.distanceSquared(b);
        if (lengthSquared < 1e-10) return 0;
        return Math.clamp(((p.x() - a.x()) * (b.x() - a.x()) + (p.y() - a.y()) * (b.y() - a.y())
                + (p.z() - a.z()) * (b.z() - a.z())) / lengthSquared, 0, 1);
    }

    /** Join the closest segment and follow the recorded direction to its destination. */
    public static List<Point> join(Point from, List<Point> points) {
        if (points.isEmpty()) throw new IllegalArgumentException("Empty route");
        List<Point> result = new ArrayList<>();
        result.add(from);
        if (points.size() == 1) {
            addDistinct(result, points.getFirst());
            return List.copyOf(result);
        }
        int nearest = 0;
        double best = Double.POSITIVE_INFINITY;
        Point joined = points.getFirst();
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i), b = points.get(i + 1);
            Point projected = a.interpolate(b, projection(from, a, b));
            double distance = from.distanceSquared(projected);
            if (distance < best) { best = distance; nearest = i; joined = projected; }
        }
        addDistinct(result, joined);
        for (int i = nearest + 1; i < points.size(); i++) addDistinct(result, points.get(i));
        return List.copyOf(result);
    }

    private static void addDistinct(List<Point> points, Point point) {
        if (points.getLast().distanceSquared(point) > 1e-8) points.add(point);
    }

    public static final class Path {
        private final List<Point> points;
        private final double[] cumulative;
        public Path(List<Point> points) {
            if (points.isEmpty()) throw new IllegalArgumentException("Empty route");
            this.points = List.copyOf(points);
            cumulative = new double[points.size()];
            for (int i = 1; i < points.size(); i++) cumulative[i] = cumulative[i - 1] + Math.sqrt(points.get(i - 1).distanceSquared(points.get(i)));
        }
        public double length() { return cumulative[cumulative.length - 1]; }
        public Point end() { return points.getLast(); }
        public double progress(Point player) {
            double best = Double.POSITIVE_INFINITY, progress = 0;
            for (int i = 1; i < points.size(); i++) {
                Point a = points.get(i - 1), b = points.get(i);
                double t = projection(player, a, b);
                double distance = player.distanceSquared(a.interpolate(b, t));
                if (distance < best) { best = distance; progress = cumulative[i - 1] + (cumulative[i] - cumulative[i - 1]) * t; }
            }
            return progress;
        }
        public Point at(double distance) {
            distance = Math.clamp(distance, 0, length());
            int index = java.util.Arrays.binarySearch(cumulative, distance);
            if (index >= 0) return points.get(index);
            index = -index - 1;
            if (index == 0) return points.getFirst();
            if (index >= points.size()) return end();
            double segment = cumulative[index] - cumulative[index - 1];
            return points.get(index - 1).interpolate(points.get(index), segment <= 1e-10 ? 0 : (distance - cumulative[index - 1]) / segment);
        }
    }
}
