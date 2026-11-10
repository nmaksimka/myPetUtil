package io.github.nmaksimka.mypetutil.platform;

import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.Monitor;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.PhysRect;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.WindowInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Переводит физические пиксели Windows в логические точки JavaFX.
 * При масштабе 125% или 150% они отличаются, а у разных мониторов масштаб может быть разным,
 * поэтому каждый монитор пересчитывается отдельно.
 */
public final class ScreenMapper {

    /** Монитор так, как его видит JavaFX. */
    public record FxScreen(Rect bounds, Rect visual) {
    }

    /** Результат: мониторы и окна уже в логических координатах. */
    public record Mapped(List<ScreenInfo> screens, List<WindowInfo> windows) {
    }

    private record Pair(Monitor monitor, FxScreen fx) {
    }

    private ScreenMapper() {
    }

    public static Mapped map(DesktopSnapshot snapshot, List<FxScreen> fxScreens) {
        List<Pair> pairs = pair(snapshot.monitors(), fxScreens);

        List<ScreenInfo> screens = new ArrayList<>();
        for (int i = 0; i < fxScreens.size(); i++) {
            FxScreen fx = fxScreens.get(i);
            boolean fullscreen = false;
            if (snapshot.fullscreenRect() != null) {
                for (Pair pair : pairs) {
                    if (pair.fx() == fx && pair.monitor().bounds().equals(snapshot.fullscreenRect())) {
                        fullscreen = true;
                    }
                }
            }
            screens.add(new ScreenInfo(i, fx.bounds(), fx.visual(), fullscreen));
        }

        List<WindowInfo> windows = new ArrayList<>();
        if (!pairs.isEmpty()) {
            for (RawWindow raw : snapshot.windows()) {
                PhysRect r = raw.rect();
                Pair pair = pairFor(pairs, (r.left() + r.right()) / 2, (r.top() + r.bottom()) / 2);
                windows.add(new WindowInfo(raw.handle(), toLogical(pair, r), raw.exe(), raw.title()));
            }
        }
        return new Mapped(screens, windows);
    }

    /**
     * Сопоставляет мониторы Windows и JavaFX. Порядок слева направо (и сверху вниз)
     * при пересчёте масштаба не меняется, поэтому достаточно отсортировать оба списка.
     */
    static List<Pair> pair(List<Monitor> monitors, List<FxScreen> fxScreens) {
        if (monitors.isEmpty() || monitors.size() != fxScreens.size()) {
            return List.of();
        }
        List<Monitor> sortedMonitors = new ArrayList<>(monitors);
        sortedMonitors.sort(Comparator.<Monitor>comparingInt(m -> m.bounds().left()).thenComparingInt(m -> m.bounds().top()));
        List<FxScreen> sortedFx = new ArrayList<>(fxScreens);
        sortedFx.sort(Comparator.<FxScreen>comparingDouble(s -> s.bounds().minX()).thenComparingDouble(s -> s.bounds().minY()));
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < sortedMonitors.size(); i++) {
            pairs.add(new Pair(sortedMonitors.get(i), sortedFx.get(i)));
        }
        return pairs;
    }

    private static Pair pairFor(List<Pair> pairs, int x, int y) {
        Pair best = pairs.get(0);
        long bestDistance = Long.MAX_VALUE;
        for (Pair pair : pairs) {
            PhysRect b = pair.monitor().bounds();
            if (b.contains(x, y)) {
                return pair;
            }
            long dx = Math.max(Math.max(b.left() - x, 0), x - b.right());
            long dy = Math.max(Math.max(b.top() - y, 0), y - b.bottom());
            long distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pair;
            }
        }
        return best;
    }

    private static Rect toLogical(Pair pair, PhysRect r) {
        PhysRect m = pair.monitor().bounds();
        Rect fx = pair.fx().bounds();
        double sx = fx.width() / m.width();
        double sy = fx.height() / m.height();
        return new Rect(
                fx.minX() + (r.left() - m.left()) * sx,
                fx.minY() + (r.top() - m.top()) * sy,
                fx.minX() + (r.right() - m.left()) * sx,
                fx.minY() + (r.bottom() - m.top()) * sy);
    }
}
