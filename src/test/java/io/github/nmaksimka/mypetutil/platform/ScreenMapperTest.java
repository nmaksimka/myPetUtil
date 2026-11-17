package io.github.nmaksimka.mypetutil.platform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final boolean PRIMARY = true;
    private static final boolean SECONDARY = false;

    @Test
    void scale150PercentIsConverted() {
        // ноутбук 2880x1800 при масштабе 150% — для JavaFX это 1920x1200
        Monitor monitor = new Monitor(new PhysRect(0, 0, 2880, 1800), new PhysRect(0, 0, 2880, 1728), PRIMARY);
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1200), new Rect(0, 0, 1920, 1152), PRIMARY);
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
        Monitor left = new Monitor(new PhysRect(0, 0, 1920, 1080), new PhysRect(0, 0, 1920, 1040), PRIMARY);
        Monitor right = new Monitor(new PhysRect(1920, 0, 5760, 2160), new PhysRect(1920, 0, 5760, 2160), SECONDARY);
        FxScreen fxLeft = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040), PRIMARY);
        FxScreen fxRight = new FxScreen(new Rect(1920, 0, 3840, 1080), new Rect(1920, 0, 3840, 1080), SECONDARY);
        RawWindow window = new RawWindow(2, new PhysRect(2920, 400, 3920, 1400), "idea64.exe", "IDEA");

        Mapped mapped = ScreenMapper.map(new DesktopSnapshot(List.of(right, left), List.of(window), null, null),
                List.of(fxLeft, fxRight));

        Rect rect = mapped.windows().get(0).rect();
        assertEquals(2420, rect.minX(), 0.001);
        assertEquals(700, rect.maxY(), 0.001);
    }

    /**
     * Раскладка, на которой нашлась ошибка: ноутбук 2560x1600 (150%) под основным монитором.
     * У Windows он начинается левее основного (x = -319), а у JavaFX — правее (x = 107).
     * Сортировка «слева направо» путала мониторы местами.
     */
    @Test
    void laptopBelowPrimaryIsNotSwapped() {
        Monitor primary = new Monitor(new PhysRect(0, 0, 1920, 1080), new PhysRect(0, 0, 1920, 1032), PRIMARY);
        Monitor laptop = new Monitor(new PhysRect(-319, 1080, 2241, 2680), new PhysRect(-319, 1080, 2241, 2608), SECONDARY);
        FxScreen fxPrimary = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1032), PRIMARY);
        FxScreen fxLaptop = new FxScreen(new Rect(107, 1080, 1814, 2147), new Rect(107, 1080, 1814, 2099), SECONDARY);
        RawWindow onLaptop = new RawWindow(3, new PhysRect(0, 1200, 1000, 1800), "code.exe", "VS Code");

        Mapped mapped = ScreenMapper.map(new DesktopSnapshot(List.of(laptop, primary), List.of(onLaptop), null, null),
                List.of(fxPrimary, fxLaptop));

        Rect rect = mapped.windows().get(0).rect();
        assertEquals(107 + 319 / 1.5, rect.minX(), 0.5);
        assertEquals(1080 + 720 / 1.5, rect.maxY(), 0.5);
        assertArrayEquals(new int[] {1000, 500}, mapped.toPhysical(1000, 500));
        assertArrayEquals(new int[] {270, 2460}, mapped.toPhysical(500, 2000));
    }

    @Test
    void fullscreenMonitorIsMarked() {
        Monitor monitor = new Monitor(new PhysRect(0, 0, 1920, 1080), new PhysRect(0, 0, 1920, 1040), PRIMARY);
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040), PRIMARY);

        Mapped mapped = ScreenMapper.map(
                new DesktopSnapshot(List.of(monitor), List.of(), new PhysRect(0, 0, 1920, 1080), "game.exe"), List.of(fx));

        assertTrue(mapped.screens().get(0).fullscreenApp());
    }

    @Test
    void emptySnapshotStillGivesScreens() {
        FxScreen fx = new FxScreen(new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040), PRIMARY);

        Mapped mapped = ScreenMapper.map(DesktopSnapshot.EMPTY, List.of(fx));

        assertEquals(1, mapped.screens().size());
        assertFalse(mapped.screens().get(0).fullscreenApp());
        assertTrue(mapped.windows().isEmpty());
        assertNull(mapped.toPhysical(10, 10), "without monitors we cannot convert, JavaFX moves the window itself");
    }
}
