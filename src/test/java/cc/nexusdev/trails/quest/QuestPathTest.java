package cc.nexusdev.trails.quest;

import cc.nexusdev.trails.Route;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestPathTest {
    private Route.Point p(double x, double z) { return new Route.Point(x, 64, z); }
    private Route road() { return new Route("road", "world", List.of(p(0, 0), p(10, 0), p(10, 10))); }

    @Test void followsRecordedCornerAndUpdatesMovingTarget() {
        var first = QuestPath.build(road(), p(0, 0), p(11, 10), 2, 3).orElseThrow();
        assertEquals(p(10, 0), first.at(10));
        assertEquals(p(10, 5), first.at(15));
        assertEquals(p(11, 10), first.end());
        var moved = QuestPath.build(road(), p(0, 0), p(12, 10), 2, 3).orElseThrow();
        assertEquals(p(12, 10), moved.end());
    }

    @Test void refusesLongConnectorsAndSingleWaypoints() {
        assertTrue(QuestPath.build(road(), p(-10, 0), p(10, 10), 2, 3).isEmpty());
        assertTrue(QuestPath.build(road(), p(0, 0), p(30, 10), 2, 3).isEmpty());
        var waypoint = new Route("point", "world", List.of(p(10, 10)));
        assertTrue(QuestPath.build(waypoint, p(9, 10), p(10, 10), 2, 3).isEmpty());
    }

    @Test void joinsNearbySegmentWithoutBacktrackingToStart() {
        var path = QuestPath.build(road(), p(5, 1), p(10, 10), 2, 3).orElseThrow();
        assertEquals(p(5, 0), path.at(1));
        assertEquals(16, path.length(), 1e-8);
    }

    @Test void restoreRulesDoNotReviveFinishedOrAlreadyStartedQuests() {
        assertTrue(BeautyQuestsProgress.matches(true, false, false));
        assertFalse(BeautyQuestsProgress.matches(false, false, false));
        assertFalse(BeautyQuestsProgress.matches(true, true, false));
        assertFalse(BeautyQuestsProgress.matches(true, false, true));
    }
}
