package io.github.nmaksimka.mypetutil.sprite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Рисует кадры одного персонажа. Все персонажи смотрят вправо,
 * зеркалирование делает рендерер. Низ сетки (y = 15) — это земля.
 */
public abstract class CharacterArt {

    public static final int SIZE = 16;
    public static final int GROUND = SIZE - 1;

    protected static final int TEAR = PixelGrid.rgb(0x7CC8FF);

    private final Map<Pose, PixelGrid> cache = new ConcurrentHashMap<>();

    /** Готовый кадр. Возвращается общий объект из кеша — его нельзя менять. */
    public PixelGrid frame(Pose pose) {
        return cache.computeIfAbsent(pose, p -> {
            PixelGrid grid = new PixelGrid(SIZE, SIZE);
            draw(grid, p, Face.of(p));
            return grid;
        });
    }

    protected abstract void draw(PixelGrid g, Pose pose, Face face);

    /** Палитра из пар 'символ', цвет: palette('o', 0xD97757, 'k', 0x000000). */
    protected static Map<Character, Integer> palette(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("palette needs pairs");
        }
        Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Character) pairs[i], PixelGrid.rgb((Integer) pairs[i + 1]));
        }
        return map;
    }

    /**
     * Глаз размером 1x2 пикселя — у пупса и призрака такие.
     * (x, y) — верхний пиксель глаза.
     */
    protected static void tallEye(PixelGrid g, Face face, int x, int y, int color, boolean tear) {
        switch (face) {
            case NORMAL -> g.vline(x, y, 2, color);
            case BLINK -> g.px(x, y + 1, color);
            case HAPPY, EATING -> {
                g.px(x - 1, y + 1, color);
                g.px(x, y, color);
                g.px(x + 1, y + 1, color);
            }
            case SURPRISED -> g.vline(x, y - 1, 3, color);
            case SAD -> {
                g.px(x - 1, y, color);
                g.px(x, y + 1, color);
                g.px(x + 1, y, color);
                if (tear) {
                    g.px(x, y + 2, TEAR);
                    g.px(x, y + 3, TEAR);
                }
            }
        }
    }
}
