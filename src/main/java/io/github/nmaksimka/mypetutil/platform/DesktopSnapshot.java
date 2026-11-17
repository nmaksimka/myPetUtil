package io.github.nmaksimka.mypetutil.platform;

import java.util.List;

/**
 * Что сейчас на рабочем столе — в физических пикселях, как отдаёт Windows.
 * В логические координаты JavaFX переводит {@link ScreenMapper}.
 *
 * @param monitors        мониторы
 * @param windows         окна сверху вниз по z-порядку
 * @param fullscreenRect  границы полноэкранного окна на переднем плане или null
 * @param foregroundExe   программа на переднем плане
 */
public record DesktopSnapshot(List<Monitor> monitors, List<RawWindow> windows,
                              PhysRect fullscreenRect, String foregroundExe) {

    public static final DesktopSnapshot EMPTY = new DesktopSnapshot(List.of(), List.of(), null, null);

    public record PhysRect(int left, int top, int right, int bottom) {

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public boolean contains(int x, int y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        public boolean covers(PhysRect other) {
            return left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom;
        }
    }

    /** Монитор: весь экран, рабочая область без панели задач и основной ли он. */
    public record Monitor(PhysRect bounds, PhysRect work, boolean primary) {
    }

    public record RawWindow(long handle, PhysRect rect, String exe, String title) {
    }
}
