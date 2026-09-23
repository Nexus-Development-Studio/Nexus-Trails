package cc.nexusdev.trails;

import org.junit.jupiter.api.Test;
import java.util.List;
import cc.nexusdev.trails.Route.Point;
import static org.junit.jupiter.api.Assertions.*;

class RouteGeometryTest {
    @Test void waypointDrawsFromPlayerToDestination() {
        var start = new Point(0, 64, 0);
        var target = new Point(10, 64, 0);
        var path = new RouteGeometry.Path(RouteGeometry.join(start, List.of(target)));
        assertEquals(10, path.length(), .001);
        assertEquals(new Point(5, 64, 0), path.at(5));
        assertEquals(target, path.end());
    }
    @Test void joinsNearestSegmentInsteadOfReturningToRouteStart() {
        var route = List.of(new Point(0, 64, 0), new Point(10, 64, 0), new Point(10, 64, 10));
        var joined = RouteGeometry.join(new Point(12, 64, 5), route);
        assertEquals(List.of(new Point(12, 64, 5), new Point(10, 64, 5), new Point(10, 64, 10)), joined);
        assertEquals(7, new RouteGeometry.Path(joined).length(), .001);
    }
    @Test void followsCornerInsteadOfCuttingAcrossBuildings() {
        var path = new RouteGeometry.Path(List.of(new Point(0, 64, 0), new Point(10, 64, 0), new Point(10, 64, 10)));
        assertEquals(new Point(10, 64, 5), path.at(15));
        assertEquals(15, path.progress(new Point(11, 64, 5)), .001);
    }
    @Test void duplicatePointsAndArrivalRemainFinite() {
        var point = new Point(-8, -20, 7);
        var path = new RouteGeometry.Path(RouteGeometry.join(point, List.of(point, point)));
        assertEquals(0, path.length());
        assertEquals(point, path.at(100));
        assertEquals(0, path.progress(point));
    }
    @Test void rejectsInvalidRouteData() {
        assertThrows(IllegalArgumentException.class, () -> new Point(Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Route("../invalid", "world", List.of(new Point(0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new Route("empty", "world", List.of()));
    }
    @Test void samplingClampsAtEnds() {
        var a = new Point(0, 0, 0); var b = new Point(0, 10, 0);
        var path = new RouteGeometry.Path(List.of(a, b));
        assertEquals(a, path.at(-10)); assertEquals(b, path.at(100));
        assertEquals(0, path.progress(new Point(0, -5, 0)));
        assertEquals(10, path.progress(new Point(0, 15, 0)));
    }
}
