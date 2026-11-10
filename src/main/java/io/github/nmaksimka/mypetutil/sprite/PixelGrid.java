package io.github.nmaksimka.mypetutil.sprite;

import java.util.Arrays;
import java.util.Map;

/**
 * Маленький холст из пикселей (ARGB). Из него собираются все кадры питомцев.
 * Координаты: x вправо, y вниз, (0,0) — левый верхний угол.
 */
public final class PixelGrid {

    public static final int TRANSPARENT = 0;

    private final int width;
    private final int height;
    private final int[] pixels;

    public PixelGrid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("size must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int get(int x, int y) {
        if (!inside(x, y)) {
            return TRANSPARENT;
        }
        return pixels[y * width + x];
    }

    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** Ставит пиксель. За границами молча игнорирует — так проще рисовать смещённые части. */
    public PixelGrid px(int x, int y, int argb) {
        if (inside(x, y)) {
            pixels[y * width + x] = argb;
        }
        return this;
    }

    public PixelGrid rect(int x, int y, int w, int h, int argb) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                px(xx, yy, argb);
            }
        }
        return this;
    }

    public PixelGrid hline(int x, int y, int w, int argb) {
        return rect(x, y, w, 1, argb);
    }

    public PixelGrid vline(int x, int y, int h, int argb) {
        return rect(x, y, 1, h, argb);
    }

    /**
     * Рисует картинку из строк. Каждый символ ищется в палитре, '.' и пробел — прозрачные.
     *
     * @throws IllegalArgumentException если в строке символ, которого нет в палитре
     */
    public PixelGrid stamp(String[] rows, int x, int y, Map<Character, Integer> palette) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch == '.' || ch == ' ') {
                    continue;
                }
                Integer color = palette.get(ch);
                if (color == null) {
                    throw new IllegalArgumentException("no color for '" + ch + "' in row " + row);
                }
                px(x + col, y + row, color);
            }
        }
        return this;
    }

    /** Заменяет все пиксели одного цвета другим (удобно для перекраски). */
    public PixelGrid replace(int from, int to) {
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == from) {
                pixels[i] = to;
            }
        }
        return this;
    }

    /** Сдвигает всё содержимое по вертикали, освободившееся место становится прозрачным. */
    public PixelGrid shiftY(int dy) {
        if (dy == 0) {
            return this;
        }
        int[] copy = Arrays.copyOf(pixels, pixels.length);
        Arrays.fill(pixels, TRANSPARENT);
        for (int y = 0; y < height; y++) {
            int ny = y + dy;
            if (ny < 0 || ny >= height) {
                continue;
            }
            System.arraycopy(copy, y * width, pixels, ny * width, width);
        }
        return this;
    }

    public boolean isOpaque(int x, int y) {
        return (get(x, y) >>> 24) != 0;
    }

    public int[] toArgbArray() {
        return Arrays.copyOf(pixels, pixels.length);
    }

    /** Цвет из #RRGGBB, полностью непрозрачный. */
    public static int rgb(int rgb) {
        return 0xFF000000 | rgb;
    }
}
