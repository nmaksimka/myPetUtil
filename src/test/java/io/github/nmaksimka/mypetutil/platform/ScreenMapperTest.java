package io.github.nmaksimka.mypetutil.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.Monitor;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.PhysRect;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;
import io.github.nmaksimka.mypetutil.platform.ScreenMapper.FxScreen;
import io.github.nmaksimka.mypetutil.platform.ScreenMapper.Mapped;
import io.github.nmaksimka.mypetutil.world.Rect;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenMapperTest {

    @Test
    void scale150PercentIsConverted() {
        // ноутбук 2880x1800 при масштабе 150% — для JavaFX это 1920x1200
        Monitor monitor = new Monitor(new PhysRect(0, 0, 2880, 1800), new PhysRect(0, 0, 2880, 1728));
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1200), new Rect(0, 0, 1920, 1152));
        RawWindow window = new RawWindow(1, new PhysRect(300, 150, 1500, 1050), "chrome.exe", "Chrome");

        Mapped mapped = ScreenMapper.map(new DesktopSnapshot(List.of(monitor), List.of(window), null, null), List.of(fx));

        Rect rect = mapped.windows().get(0).rect();
        assertEquals(200, rect.minX(), 0.001);
        assertEquals(100, rect.minY(), 0.001);
        assertEquals(1000, rect.maxX(), 0.001);
        assertEquals(700, rect.maxY(), 0.001);
    }

    @Test
    void windowOnSecondMonitorWithOtherScale() {
        Monitor left = new Monitor(new PhysRect(0, 0, 1920, 1080), new PhysRect(0, 0, 1920, 1040));
        Monitor right = new Monitor(new PhysRect(1920, 0, 5760, 2160), new PhysRect(1920, 0, 5760, 2160));
        FxScreen fxLeft = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040));
        FxScreen fxRight = new FxScreen(new Rect(1920, 0, 3840, 1080), new Rect(1920, 0, 3840, 1080));
        RawWindow window = new RawWindow(2, new PhysRect(2920, 400, 3920, 1400), "idea64.exe", "IDEA");

        Mapped mapped = ScreenMapper.map(new DesktopSnapshot(List.of(right, left), List.of(window), null, null),
                List.of(fxLeft, fxRight));

        Rect rect = mapped.windows().get(0).rect();
        assertEquals(2420, rect.minX(), 0.001);
        assertEquals(700, rect.maxY(), 0.001);
    }

    @Test
    void fullscreenMonitorIsMarked() {
        Monitor monitor = new Monitor(new PhysRect(0, 0, 1920, 1080), new PhysRect(0, 0, 1920, 1040));
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040));

        Mapped mapped = ScreenMapper.map(
                new DesktopSnapshot(List.of(monitor), List.of(), new PhysRect(0, 0, 1920, 1080), "game.exe"), List.of(fx));

        assertTrue(mapped.screens().get(0).fullscreenApp());
    }

    @Test
    void emptySnapshotStillGivesScreens() {
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040));

        Mapped mapped = ScreenMapper.map(DesktopSnapshot.EMPTY, List.of(fx));

        assertEquals(1, mapped.screens().size());
        assertFalse(mapped.screens().get(0).fullscreenApp());
        assertTrue(mapped.windows().isEmpty());
    }
}
