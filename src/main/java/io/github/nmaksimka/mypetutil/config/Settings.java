package io.github.nmaksimka.mypetutil.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Все настройки утилиты. Хранится в settings.json, поэтому поля простые и публичные
 * (Gson работает с полями напрямую). Значения по умолчанию — для первого запуска.
 */
public class Settings {

    public static final int MIN_SCALE = 2;
    public static final int MAX_SCALE = 6;

    public BehaviorMode mode = BehaviorMode.WINDOWS;

    /** Сколько экранных точек занимает один пиксель спрайта. 3 — маленький питомец 48x48. */
    public int scale = 3;

    /** Множитель скорости ходьбы, 0.5..2. */
    public double speed = 1.0;

    /** Можно ли уходить на другие мониторы. */
    public boolean multiMonitor = true;

    public boolean hungerEnabled = false;
    public HungerSpeed hungerSpeed = HungerSpeed.NORMAL;

    /** Программы (имя exe в нижнем регистре), по окнам которых питомцам гулять нельзя. */
    public Set<String> blockedApps = new TreeSet<>();

    /** Прятать питомцев, когда запрещённая программа открыта на весь экран. */
    public boolean hideOnBlockedFullscreen = true;

    public Theme theme = Theme.SYSTEM;

    public boolean autostart = false;

    public List<PetEntry> pets = new ArrayList<>();

    /** Приводит значения к допустимым — на случай, если json поправили руками. */
    public Settings normalize() {
        if (mode == null) {
            mode = BehaviorMode.WINDOWS;
        }
        if (hungerSpeed == null) {
            hungerSpeed = HungerSpeed.NORMAL;
        }
        if (theme == null) {
            theme = Theme.SYSTEM;
        }
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        if (Double.isNaN(speed) || speed < 0.5 || speed > 2.0) {
            speed = Math.max(0.5, Math.min(2.0, Double.isNaN(speed) ? 1.0 : speed));
        }
        if (blockedApps == null) {
            blockedApps = new TreeSet<>();
        } else {
            Set<String> lower = new TreeSet<>();
            for (String app : blockedApps) {
                if (app != null && !app.isBlank()) {
                    lower.add(app.toLowerCase());
                }
            }
            blockedApps = lower;
        }
        if (pets == null) {
            pets = new ArrayList<>();
        }
        pets.removeIf(p -> p == null || p.character == null);
        for (PetEntry pet : pets) {
            pet.satiety = Math.max(0, Math.min(PetEntry.MAX_SATIETY, pet.satiety));
        }
        return this;
    }
}
