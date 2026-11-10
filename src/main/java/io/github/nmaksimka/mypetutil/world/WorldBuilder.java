package io.github.nmaksimka.mypetutil.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Собирает полы из мониторов и окон.
 * Окна приходят в порядке «сверху вниз» (как их отдаёт EnumWindows): если низ окна
 * закрыт окном выше, по закрытому куску ходить нельзя.
 */
public final class WorldBuilder {

    /** Отступ от боковых рамок окна, чтобы питомец не стоял на самой границе. */
    private static final double EDGE_INSET = 2;

    private WorldBuilder() {
    }

    public static World build(List<ScreenInfo> screens, List<WindowInfo> windowsTopFirst, boolean withWindows,
                              Set<String> blockedApps, double petWidth, double petHeight) {
        List<Floor> floors = new ArrayList<>();
        for (ScreenInfo screen : screens) {
            floors.add(new Floor("screen:" + screen.index(), FloorKind.SCREEN,
                    screen.floorMinX(), screen.floorMaxX(), screen.floorY(), screen.floorMinX(), screen.index()));
        }
        if (withWindows) {
            for (int i = 0; i < windowsTopFirst.size(); i++) {
                WindowInfo window = windowsTopFirst.get(i);
                if (blockedApps.contains(window.exe())) {
                    continue;
                }
                addWindowFloors(floors, screens, windowsTopFirst, i, petWidth, petHeight);
            }
        }
        return new World(screens, floors);
    }

    private static void addWindowFloors(List<Floor> floors, List<ScreenInfo> screens, List<WindowInfo> windows,
                                        int index, double petWidth, double petHeight) {
        WindowInfo window = windows.get(index);
        Rect rect = window.rect();
        double y = rect.maxY();
        if (rect.height() < petHeight) {
            return;
        }
        ScreenInfo screen = screenOf(screens, rect.centerX(), y - 1);
        if (screen == null || screen.fullscreenApp()) {
            return;
        }
        // окно, развёрнутое на весь экран, совпадает с полом монитора — второй пол не нужен
        if (y > screen.floorY() - 4 || y - petHeight < screen.visual().minY()) {
            return;
        }

        List<double[]> segments = new ArrayList<>();
        segments.add(new double[] {
            Math.max(rect.minX(), screen.bounds().minX()) + EDGE_INSET,
            Math.min(rect.maxX(), screen.bounds().maxX()) - EDGE_INSET
        });
        double probeY = y - 1;
        for (int above = 0; above < index; above++) {
            Rect cover = windows.get(above).rect();
            if (cover.minY() < probeY && cover.maxY() > probeY) {
                segments = subtract(segments, cover.minX(), cover.maxX());
            }
        }
        String owner = "win:" + window.handle();
        for (double[] segment : segments) {
            if (segment[1] - segment[0] >= petWidth * 1.5) {
                floors.add(new Floor(owner, FloorKind.WINDOW, segment[0], segment[1], y, rect.minX(), screen.index()));
            }
        }
    }

    /** Вырезает из отрезков кусок [from, to]. */
    static List<double[]> subtract(List<double[]> segments, double from, double to) {
        List<double[]> result = new ArrayList<>();
        for (double[] s : segments) {
            if (to <= s[0] || from >= s[1]) {
                result.add(s);
                continue;
            }
            if (from > s[0]) {
                result.add(new double[] {s[0], from});
            }
            if (to < s[1]) {
                result.add(new double[] {to, s[1]});
            }
        }
        return result;
    }

    private static ScreenInfo screenOf(List<ScreenInfo> screens, double x, double y) {
        for (ScreenInfo screen : screens) {
            if (screen.bounds().contains(x, y)) {
                return screen;
            }
        }
        return null;
    }
}
