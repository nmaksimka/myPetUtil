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

    private final Pet pet;
    private double pixel;

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

    private double spriteToStageX() {
        return pet.x() - SIDE * pixel;
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

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        if (pet.clipMin() != null) {
            // питомец заходит в окно или выходит из него: видна только часть над полом
            double from = pet.clipMin() - stageX;
            double to = pet.clipMax() - stageX;
            gc.beginPath();
            gc.rect(from, 0, Math.max(0, to - from), canvas.getHeight());
            gc.clip();
        }
        if (pet.state() == PetState.DRAG) {
            double angle = Math.max(-16, Math.min(16, -pet.vx() / 70));
            gc.translate(ox + size / 2, oy);
            gc.rotate(angle);
            gc.translate(-(ox + size / 2), -oy);
        }
        Pose pose = pet.pose();
        PixelGrid frame = pet.character().art().frame(pose);
        SpriteImages.draw(gc, frame, ox, oy, size, size, pet.facing() < 0);
        gc.restore();

        double headY = oy + topOfSprite(frame) * pixel;
        double iconY = headY - 9 * pixel;
        if (pet.heartTimer() > 0) {
            double bob = Math.sin(time * 6) * pixel;
            SpriteImages.draw(gc, Emotes.HEART, ox + size / 2 - 3.5 * pixel, iconY + bob,
                    7 * pixel, 6 * pixel, false);
        } else if (pet.questionTimer() > 0) {
            SpriteImages.draw(gc, Emotes.QUESTION, ox + size / 2 - 2.5 * pixel, iconY - pixel,
                    5 * pixel, 7 * pixel, false);
        }
        if (hungerEnabled && (pet.hovered() || pet.state() == PetState.SAD)) {
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
