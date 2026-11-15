package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.overlay.SpriteImages;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/** Живая картинка персонажа в окне настроек. Анимацию двигает общий таймер окна. */
public final class SpritePreview extends Canvas {

    private PetCharacter character;
    private final double pixel;
    private Anim anim = Anim.IDLE;
    private double animStart;
    private boolean mirror;
    private PixelGrid lastGrid;
    private boolean lastMirror;

    public SpritePreview(PetCharacter character, double pixel) {
        super(16 * pixel, 16 * pixel);
        this.character = character;
        this.pixel = pixel;
        draw(0);
    }

    public void setAnim(Anim anim, double now) {
        if (this.anim != anim) {
            this.anim = anim;
            this.animStart = now;
        }
    }

    public void setCharacter(PetCharacter character) {
        this.character = character;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public void tick(double now) {
        draw(now);
    }

    private void draw(double now) {
        double t = now - animStart;
        int frame = (int) (t / anim.frameSeconds());
        // моргает примерно раз в 3 секунды
        boolean blink = anim == Anim.IDLE && (now % 3.2) < 0.14;
        PixelGrid grid = character.art().frame(new Pose(anim, frame, blink));
        // кадр сменился? если нет — холст уже правильный, не трогаем его
        if (grid == lastGrid && mirror == lastMirror) {
            return;
        }
        lastGrid = grid;
        lastMirror = mirror;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        SpriteImages.draw(gc, grid, 0, 0, 16 * pixel, 16 * pixel, mirror);
    }

    /** Статичная картинка сетки (еда, иконки). */
    public static Canvas still(PixelGrid grid, double pixel) {
        Canvas canvas = new Canvas(grid.width() * pixel, grid.height() * pixel);
        SpriteImages.draw(canvas.getGraphicsContext2D(), grid, 0, 0, canvas.getWidth(), canvas.getHeight(), false);
        return canvas;
    }
}
