package io.github.nmaksimka.mypetutil.overlay;

import io.github.nmaksimka.mypetutil.sprite.PixelGrid;

import java.util.Map;
import java.util.WeakHashMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * Превращает пиксельные сетки в картинки JavaFX (1 пиксель сетки = 1 пиксель картинки)
 * и рисует их увеличенными без размытия.
 */
public final class SpriteImages {

    private static final Map<PixelGrid, Image> CACHE = new WeakHashMap<>();

    private SpriteImages() {
    }

    public static Image of(PixelGrid grid) {
        return CACHE.computeIfAbsent(grid, g -> {
            WritableImage image = new WritableImage(g.width(), g.height());
            image.getPixelWriter().setPixels(0, 0, g.width(), g.height(),
                    PixelFormat.getIntArgbInstance(), g.toArgbArray(), 0, g.width());
            return image;
        });
    }

    /**
     * Рисует сетку в прямоугольник (x, y, w, h).
     *
     * @param mirror отразить по горизонтали (питомец смотрит влево)
     */
    public static void draw(GraphicsContext gc, PixelGrid grid, double x, double y, double w, double h, boolean mirror) {
        gc.setImageSmoothing(false);
        Image image = of(grid);
        if (mirror) {
            gc.save();
            gc.translate(x + w, y);
            gc.scale(-1, 1);
            gc.drawImage(image, 0, 0, w, h);
            gc.restore();
        } else {
            gc.drawImage(image, x, y, w, h);
        }
    }
}
