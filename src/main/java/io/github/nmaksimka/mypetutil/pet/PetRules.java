package io.github.nmaksimka.mypetutil.pet;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.Settings;

/**
 * Настройки, которые нужны логике питомца на каждом шаге.
 *
 * @param pixel           размер одного пикселя спрайта в экранных точках
 * @param speed           общий множитель скорости из настроек
 * @param hungerPerSecond сколько сытости уходит за секунду
 */
public record PetRules(BehaviorMode mode, double pixel, double speed, boolean multiMonitor,
                       boolean hungerEnabled, double hungerPerSecond) {

    /** Базовая скорость ходьбы в пикселях спрайта в секунду. */
    public static final double WALK_PIXELS_PER_SECOND = 20;

    public static PetRules from(Settings settings) {
        return new PetRules(settings.mode, settings.scale, settings.speed, settings.multiMonitor,
                settings.hungerEnabled, settings.hungerSpeed.perSecond());
    }

    public double size() {
        return 16 * pixel;
    }

    public boolean walksOnWindows() {
        return mode == BehaviorMode.WINDOWS;
    }
}
