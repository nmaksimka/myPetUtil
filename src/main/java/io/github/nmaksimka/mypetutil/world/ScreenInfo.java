package io.github.nmaksimka.mypetutil.world;

/**
 * Монитор.
 *
 * @param bounds        весь экран
 * @param visual        экран без панели задач
 * @param fullscreenApp на этом мониторе сейчас открыта программа на весь экран (игра, видео)
 */
public record ScreenInfo(int index, Rect bounds, Rect visual, boolean fullscreenApp) {

    /** Где стоит пол: над панелью задач, а при полноэкранной программе — у самого низа монитора. */
    public double floorY() {
        return fullscreenApp ? bounds.maxY() : visual.maxY();
    }

    public double floorMinX() {
        return fullscreenApp ? bounds.minX() : visual.minX();
    }

    public double floorMaxX() {
        return fullscreenApp ? bounds.maxX() : visual.maxX();
    }
}
