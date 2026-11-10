package io.github.nmaksimka.mypetutil.world;

import java.util.ArrayList;
import java.util.List;

/** Снимок «мира» на текущий момент: мониторы и все полы, по которым можно ходить. */
public final class World {

    private final List<ScreenInfo> screens;
    private final List<Floor> floors;

    public World(List<ScreenInfo> screens, List<Floor> floors) {
        if (screens.isEmpty()) {
            throw new IllegalArgumentException("world needs at least one screen");
        }
        this.screens = List.copyOf(screens);
        this.floors = List.copyOf(floors);
    }

    public List<ScreenInfo> screens() {
        return screens;
    }

    public List<Floor> floors() {
        return floors;
    }

    public Floor screenFloor(int screen) {
        String owner = "screen:" + screen;
        for (Floor floor : floors) {
            if (floor.owner().equals(owner)) {
                return floor;
            }
        }
        return null;
    }

    /** Отрезок нужного владельца, над которым стоит точка x. */
    public Floor floorAt(String owner, double x) {
        for (Floor floor : floors) {
            if (floor.owner().equals(owner) && floor.containsX(x)) {
                return floor;
            }
        }
        return null;
    }

    /** Ближайший к x отрезок владельца — нужен, когда питомец заходит или выходит за край. */
    public Floor nearestOf(String owner, double x) {
        Floor best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Floor floor : floors) {
            if (!floor.owner().equals(owner)) {
                continue;
            }
            double distance = floor.containsX(x) ? 0 : Math.min(Math.abs(floor.minX() - x), Math.abs(floor.maxX() - x));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = floor;
            }
        }
        return best;
    }

    /**
     * Куда приземлится падающий питомец: самый верхний пол под точкой x
     * между старой и новой высотой ног.
     */
    public Floor landing(double x, double fromY, double toY, boolean withWindows) {
        Floor best = null;
        for (Floor floor : floors) {
            if (!withWindows && floor.kind() == FloorKind.WINDOW) {
                continue;
            }
            if (!floor.containsX(x) || floor.y() < fromY || floor.y() > toY) {
                continue;
            }
            if (best == null || floor.y() < best.y()) {
                best = floor;
            }
        }
        return best;
    }

    /** Монитор, на котором точка, а если она между мониторами — ближайший. */
    public ScreenInfo screenAt(double x, double y) {
        ScreenInfo best = screens.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (ScreenInfo screen : screens) {
            Rect b = screen.bounds();
            if (b.contains(x, y)) {
                return screen;
            }
            double dx = Math.max(Math.max(b.minX() - x, 0), x - b.maxX());
            double dy = Math.max(Math.max(b.minY() - y, 0), y - b.maxY());
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = screen;
            }
        }
        return best;
    }

    /** Полы, куда можно перейти, кроме текущего владельца. */
    public List<Floor> exits(String exceptOwner, boolean withWindows, double minWidth) {
        List<Floor> result = new ArrayList<>();
        for (Floor floor : floors) {
            if (floor.owner().equals(exceptOwner) || floor.width() < minWidth) {
                continue;
            }
            if (!withWindows && floor.kind() == FloorKind.WINDOW) {
                continue;
            }
            result.add(floor);
        }
        return result;
    }
}
