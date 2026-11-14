package io.github.nmaksimka.mypetutil.overlay;

import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.pet.PetState;
import io.github.nmaksimka.mypetutil.sprite.Emotes;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/** Окошко одного питомца: рисует кадр, сердечки, полоску сытости и ловит мышку. */
public final class PetView extends OverlayStage {

    /** Поля вокруг спрайта в пикселях спрайта: по бокам — для наклона, сверху — для значков. */
    private static final int SIDE = 3;
    private static final int TOP = 9;

    public interface DragListener {
        void pressed(PetView view, double screenX, double screenY);

        void dragged(PetView view, double screenX, double screenY);

        void released(PetView view, double screenX, double screenY);
    }

    private static final int NONE = Integer.MIN_VALUE;

    /** Всё, что влияет на картинку питомца. Сравнивается с прошлым кадром. */
    private record RenderKey(PixelGrid frame, boolean mirror, int clipFrom, int clipTo, int angle,
                             int heartBob, boolean question, int satiety, double pixel) {
    }

    private final Pet pet;
    private double pixel;
    private RenderKey lastKey;

    public PetView(Window owner, Pet pet, double pixel, DragListener listener) {
        super(owner, "pet", width(pixel), height(pixel));
        this.pet = pet;
        this.pixel = pixel;

        canvas.setOnMouseEntered(e -> {
            pet.setHovered(true);
            canvas.setCursor(Cursor.OPEN_HAND);
        });
        canvas.setOnMouseExited(e -> {
            pet.setHovered(false);
            canvas.setCursor(Cursor.DEFAULT);
        });
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.setCursor(Cursor.CLOSED_HAND);
                listener.pressed(this, e.getScreenX(), e.getScreenY());
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                listener.dragged(this, e.getScreenX(), e.getScreenY());
            }
        });
        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.setCursor(Cursor.OPEN_HAND);
                listener.released(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private static double width(double pixel) {
        return (16 + SIDE * 2) * pixel;
    }

    private static double height(double pixel) {
        return (16 + TOP) * pixel;
    }

    public void setPixel(double pixel) {
        if (this.pixel != pixel) {
            this.pixel = pixel;
            resize(width(pixel), height(pixel));
        }
    }

    public void showAtPet() {
        show(spriteToStageX(), spriteToStageY());
    }

    /**
     * Когда питомец ходит сам, окно двигается шагами по одному пикселю спрайта.
     * Двигать прозрачное окно для Windows дорого: так сдвигов в 3-6 раз меньше,
     * а походка выглядит как в пиксельных играх. Когда его тащат или он летит — плавно.
     */
    private double spriteToStageX() {
        double x = pet.x();
        if (pet.state().grounded()) {
            x = Math.floor(x / pixel) * pixel;
        }
        return x - SIDE * pixel;
    }

    private double spriteToStageY() {
        return pet.y() - (16 + TOP) * pixel;
    }

    /** Перерисовать и переставить окно под текущее положение питомца. */
    public void render(boolean hungerEnabled, double time) {
        moveTo(spriteToStageX(), spriteToStageY());
        double size = 16 * pixel;
        double ox = SIDE * pixel;
        double oy = TOP * pixel;
        double stageX = stageX();

        // Сначала собираем всё, от чего зависит картинка. Если ничего не поменялось с прошлого
        // кадра — не перерисовываем: анимация меняет кадр 5-8 раз в секунду, а цикл крутится 60.
        Pose pose = pet.pose();
        PixelGrid frame = pet.character().art().frame(pose);
        boolean mirror = pet.facing() < 0;
        int clipFrom = pet.clipMin() == null ? Integer.MIN_VALUE : (int) Math.round(pet.clipMin() - stageX);
        int clipTo = pet.clipMax() == null ? Integer.MAX_VALUE : (int) Math.round(pet.clipMax() - stageX);
        int angle = pet.state() == PetState.DRAG ? (int) Math.round(Math.max(-16, Math.min(16, -pet.vx() / 70))) : 0;
        int heartBob = pet.heartTimer() > 0 ? (int) Math.round(Math.sin(time * 6)) : NONE;
        boolean question = heartBob == NONE && pet.questionTimer() > 0;
        int satiety = hungerEnabled && (pet.hovered() || pet.state() == PetState.SAD)
                ? (int) Math.round(pet.satiety()) : NONE;
        RenderKey key = new RenderKey(frame, mirror, clipFrom, clipTo, angle, heartBob, question, satiety, pixel);
        if (key.equals(lastKey)) {
            return;
        }
        lastKey = key;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        if (clipFrom != Integer.MIN_VALUE) {
            // питомец заходит в окно или выходит из него: видна только часть над полом
            gc.beginPath();
            gc.rect(clipFrom, 0, Math.max(0, clipTo - clipFrom), canvas.getHeight());
            gc.clip();
        }
        if (angle != 0) {
            gc.translate(ox + size / 2, oy);
            gc.rotate(angle);
            gc.translate(-(ox + size / 2), -oy);
        }
        SpriteImages.draw(gc, frame, ox, oy, size, size, mirror);
        gc.restore();

        double headY = oy + topOfSprite(frame) * pixel;
        double iconY = headY - 9 * pixel;
        if (heartBob != NONE) {
            SpriteImages.draw(gc, Emotes.HEART, ox + size / 2 - 3.5 * pixel, iconY + heartBob * pixel,
                    7 * pixel, 6 * pixel, false);
        } else if (question) {
            SpriteImages.draw(gc, Emotes.QUESTION, ox + size / 2 - 2.5 * pixel, iconY - pixel,
                    5 * pixel, 7 * pixel, false);
        }
        if (satiety != NONE) {
            drawSatiety(gc, ox + 2 * pixel, headY - 2.5 * pixel, size - 4 * pixel);
        }
    }

    private void drawSatiety(GraphicsContext gc, double x, double y, double w) {
        double h = Math.max(3, pixel);
        double part = pet.satiety() / PetEntry.MAX_SATIETY;
        gc.setFill(Color.rgb(30, 30, 36, 0.85));
        gc.fillRect(x - 1, y - 1, w + 2, h + 2);
        Color color = part > 0.5 ? Color.web("#6CCB5F") : part > 0.2 ? Color.web("#F2C232") : Color.web("#F0506E");
        gc.setFill(color);
        gc.fillRect(x, y, w * part, h);
    }

    /** Самая верхняя непрозрачная строка кадра — над ней рисуем значки. */
    private static int topOfSprite(PixelGrid grid) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.isOpaque(x, y)) {
                    return y;
                }
            }
        }
        return 0;
    }

    public Pet pet() {
        return pet;
    }
}
