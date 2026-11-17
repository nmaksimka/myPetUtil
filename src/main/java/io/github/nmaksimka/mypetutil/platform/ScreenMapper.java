package io.github.nmaksimka.mypetutil.platform;

import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.Monitor;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.PhysRect;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.WindowInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Переводит физические пиксели Windows в логические точки JavaFX.
 * При масштабе 125% или 150% они отличаются, а у разных мониторов масштаб может быть разным,
 * поэтому каждый монитор пересчитывается отдельно.
 */
public final class ScreenMapper {

    /** Монитор так, как его видит JavaFX. */
    public record FxScreen(Rect bounds, Rect visual, boolean primary) {
    }

    /** Результат: мониторы и окна уже в логических координатах. */
    public record Mapped(List<ScreenInfo> screens, List<WindowInfo> windows, List<Pair> pairs) {

        /**
         * Обратный пересчёт: логическая точка JavaFX → физические пиксели Windows.
         *
         * @return {x, y} или null, если мониторы ещё не сопоставлены
         */
        public int[] toPhysical(double x, double y) {
            if (pairs.isEmpty()) {
                return null;
            }
            Pair best = pairs.get(0);
            double bestDistance = Double.MAX_VALUE;
            for (Pair pair : pairs) {
                Rect b = pair.fx().bounds();
                double dx = Math.max(Math.max(b.minX() - x, 0), x - b.maxX());
                double dy = Math.max(Math.max(b.minY() - y, 0), y - b.maxY());
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pair;
                }
            }
            PhysRect m = best.monitor().bounds();
            Rect fx = best.fx().bounds();
            return new int[] {
                (int) Math.round(m.left() + (x - fx.minX()) * m.width() / fx.width()),
                (int) Math.round(m.top() + (y - fx.minY()) * m.height() / fx.height())
            };
        }
    }

    /** Монитор Windows и тот же монитор глазами JavaFX. */
    public record Pair(Monitor monitor, FxScreen fx) {
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
        return new Mapped(screens, windows, pairs);
    }

    /**
     * Сопоставляет мониторы Windows и JavaFX.
     * Просто отсортировать их слева направо нельзя: при разном масштабе JavaFX раскладывает
     * мониторы по-своему (например, ноутбук под основным экраном у Windows начинается левее,
     * а у JavaFX — правее). Поэтому основной монитор сопоставляется по флагу «основной»,
     * а остальные — по направлению от основного и пропорциям экрана. Перебираем все варианты
     * и берём самый похожий — мониторов обычно 1-3, это мгновенно.
     */
    static List<Pair> pair(List<Monitor> monitors, List<FxScreen> fxScreens) {
        int n = monitors.size();
        if (n == 0 || n != fxScreens.size() || n > 6) {
            return List.of();
        }
        Monitor primaryMonitor = monitors.stream().filter(Monitor::primary).findFirst().orElse(monitors.get(0));
        FxScreen primaryFx = fxScreens.stream().filter(FxScreen::primary).findFirst().orElse(fxScreens.get(0));

        int[] best = null;
        double bestCost = Double.MAX_VALUE;
        for (int[] perm : permutations(n)) {
            double cost = 0;
            for (int i = 0; i < n; i++) {
                cost += cost(monitors.get(i), fxScreens.get(perm[i]), primaryMonitor, primaryFx);
            }
            if (cost < bestCost) {
                bestCost = cost;
                best = perm;
            }
        }
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pairs.add(new Pair(monitors.get(i), fxScreens.get(best[i])));
        }
        return pairs;
    }

    /** Насколько монитор Windows не похож на монитор JavaFX. 0 — точно он. */
    private static double cost(Monitor m, FxScreen fx, Monitor primaryMonitor, FxScreen primaryFx) {
        if (m.primary() != fx.primary()) {
            return 1000;
        }
        PhysRect mb = m.bounds();
        PhysRect pb = primaryMonitor.bounds();
        Rect fb = fx.bounds();
        Rect fp = primaryFx.bounds();
        // центр монитора относительно центра основного, в долях размера основного
        double mx = ((mb.left() + mb.right()) / 2.0 - (pb.left() + pb.right()) / 2.0) / pb.width();
        double my = ((mb.top() + mb.bottom()) / 2.0 - (pb.top() + pb.bottom()) / 2.0) / pb.height();
        double fxx = (fb.centerX() - fp.centerX()) / fp.width();
        double fxy = (fb.centerY() - fp.centerY()) / fp.height();
        double direction = Math.hypot(mx - fxx, my - fxy);
        double aspect = Math.abs((double) mb.width() / mb.height() - fb.width() / fb.height());
        return direction + aspect * 2;
    }

    private static List<int[]> permutations(int n) {
        List<int[]> result = new ArrayList<>();
        permute(new int[n], new boolean[n], 0, result);
        return result;
    }

    private static void permute(int[] current, boolean[] used, int depth, List<int[]> result) {
        if (depth == current.length) {
            result.add(current.clone());
            return;
        }
        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                permute(current, used, depth + 1, result);
                used[i] = false;
            }
        }
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
