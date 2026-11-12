package io.github.nmaksimka.mypetutil.overlay;

import io.github.nmaksimka.mypetutil.pet.FoodItem;

import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;

/** Окошко с едой. Лежащую еду можно снова схватить и перенести. */
public final class FoodView extends OverlayStage {

    public interface Listener {
        void pressed(FoodView view, double screenX, double screenY);

        void dragged(FoodView view, double screenX, double screenY);

        void released(FoodView view, double screenX, double screenY);
    }

    private final FoodItem food;
    private double pixel;

    public FoodView(Window owner, FoodItem food, double pixel, Listener listener) {
        super(owner, "food", FoodItem.size(pixel), FoodItem.size(pixel));
        this.food = food;
        this.pixel = pixel;
        canvas.setCursor(Cursor.OPEN_HAND);
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                listener.pressed(this, e.getScreenX(), e.getScreenY());
            }
        });
        canvas.setOnMouseDragged(e -> listener.dragged(this, e.getScreenX(), e.getScreenY()));
        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                listener.released(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    public void showAtFood() {
        show(food.x(), food.y() - FoodItem.size(pixel));
    }

    public void setPixel(double pixel) {
        if (this.pixel != pixel) {
            this.pixel = pixel;
            resize(FoodItem.size(pixel), FoodItem.size(pixel));
        }
    }

    public void render() {
        double size = FoodItem.size(pixel);
        moveTo(food.x(), food.y() - size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, size, size);
        gc.setGlobalAlpha(food.opacity());
        SpriteImages.draw(gc, food.type().grid(), 0, 0, size, size, false);
        gc.setGlobalAlpha(1);
    }

    public FoodItem food() {
        return food;
    }
}
