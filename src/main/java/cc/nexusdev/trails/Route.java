package cc.nexusdev.trails;

import java.util.List;

public record Route(String id, String world, List<Point> points) {
    public Route {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,47}")) throw new IllegalArgumentException("Invalid destination ID");
        if (world == null || world.isBlank()) throw new IllegalArgumentException("A world is required");
        points = List.copyOf(points);
        if (points.isEmpty()) throw new IllegalArgumentException("A destination needs at least one point");
    }

    public record Point(double x, double y, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Non-finite point");
        }
        public double distanceSquared(Point p) {
            double dx = p.x - x, dy = p.y - y, dz = p.z - z;
            return dx * dx + dy * dy + dz * dz;
        }
        public Point interpolate(Point other, double t) {
            return new Point(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t);
        }
    }
}
