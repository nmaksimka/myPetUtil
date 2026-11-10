package io.github.nmaksimka.mypetutil.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorldBuilderTest {

    private static final ScreenInfo SCREEN = new ScreenInfo(0,
            new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040), false);

    @Test
    void screenFloorIsAboveTaskbar() {
        World world = WorldBuilder.build(List.of(SCREEN), List.of(), true, Set.of(), 48, 48);

        Floor floor = world.screenFloor(0);
        assertEquals(1040, floor.y());
        assertEquals(FloorKind.SCREEN, floor.kind());
    }

    @Test
    void fullscreenAppMovesFloorToScreenBottom() {
        ScreenInfo game = new ScreenInfo(0, SCREEN.bounds(), SCREEN.visual(), true);
        WindowInfo window = new WindowInfo(1, new Rect(100, 100, 900, 700), "app.exe", "App");

        World world = WorldBuilder.build(List.of(game), List.of(window), true, Set.of(), 48, 48);

        assertEquals(1080, world.screenFloor(0).y());
        assertEquals(1, world.floors().size(), "windows behind fullscreen app are ignored");
    }

    @Test
    void windowBottomBecomesFloor() {
        WindowInfo window = new WindowInfo(7, new Rect(100, 100, 900, 700), "chrome.exe", "Chrome");

        World world = WorldBuilder.build(List.of(SCREEN), List.of(window), true, Set.of(), 48, 48);

        Floor floor = world.floorAt("win:7", 500);
        assertNotNull(floor);
        assertEquals(700, floor.y());
        assertEquals(102, floor.minX());
        assertEquals(898, floor.maxX());
    }

    @Test
    void blockedAppHasNoFloor() {
        WindowInfo window = new WindowInfo(7, new Rect(100, 100, 900, 700), "game.exe", "Game");

        World world = WorldBuilder.build(List.of(SCREEN), List.of(window), true, Set.of("game.exe"), 48, 48);

        assertNull(world.floorAt("win:7", 500));
    }

    @Test
    void windowsAreIgnoredInDesktopMode() {
        WindowInfo window = new WindowInfo(7, new Rect(100, 100, 900, 700), "chrome.exe", "Chrome");

        World world = WorldBuilder.build(List.of(SCREEN), List.of(window), false, Set.of(), 48, 48);

        assertEquals(1, world.floors().size());
    }

    @Test
    void coveredPartOfBottomIsCut() {
        WindowInfo top = new WindowInfo(1, new Rect(400, 500, 600, 800), "a.exe", "Top");
        WindowInfo bottom = new WindowInfo(2, new Rect(100, 100, 900, 700), "b.exe", "Bottom");

        World world = WorldBuilder.build(List.of(SCREEN), List.of(top, bottom), true, Set.of(), 48, 48);

        assertNotNull(world.floorAt("win:2", 200));
        assertNull(world.floorAt("win:2", 500), "middle is hidden under the top window");
        assertNotNull(world.floorAt("win:2", 800));
    }

    @Test
    void maximizedWindowDoesNotDuplicateScreenFloor() {
        WindowInfo maximized = new WindowInfo(3, new Rect(0, 0, 1920, 1040), "idea64.exe", "IDEA");

        World world = WorldBuilder.build(List.of(SCREEN), List.of(maximized), true, Set.of(), 48, 48);

        assertEquals(1, world.floors().size());
    }

    @Test
    void landingPicksHighestFloorBelow() {
        WindowInfo window = new WindowInfo(7, new Rect(100, 100, 900, 700), "chrome.exe", "Chrome");
        World world = WorldBuilder.build(List.of(SCREEN), List.of(window), true, Set.of(), 48, 48);

        assertEquals(700, world.landing(500, 300, 2000, true).y());
        assertEquals(1040, world.landing(500, 300, 2000, false).y());
        assertEquals(1040, world.landing(1500, 300, 2000, true).y());
    }

    @Test
    void subtractSplitsSegment() {
        List<double[]> parts = WorldBuilder.subtract(List.of(new double[] {0, 100}), 40, 60);

        assertEquals(2, parts.size());
        assertTrue(parts.get(0)[1] == 40 && parts.get(1)[0] == 60);
    }
}
