package io.github.nmaksimka.mypetutil.overlay;

import com.sun.jna.platform.win32.WinDef.HWND;

import io.github.nmaksimka.mypetutil.platform.NativeWindows;
import io.github.nmaksimka.mypetutil.platform.WindowScanner;

import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Прозрачное окошко поверх всех окон с одним холстом внутри.
 * Из него сделаны питомцы и еда. Прозрачные пиксели пропускают клики насквозь.
 */
public class OverlayStage {

    private static final AtomicLong COUNTER = new AtomicLong();

    protected final Stage stage;
    protected final Canvas canvas;
    private final String title;
    private HWND hwnd;
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;

    public OverlayStage(Window owner, String kind, double width, double height) {
        this.title = WindowScanner.OWN_WINDOW_PREFIX + kind + "·" + COUNTER.incrementAndGet();
        canvas = new Canvas(width, height);
        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root, width, height, Color.TRANSPARENT);
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
    }

    public void show(double x, double y) {
        moveTo(x, y);
        stage.show();
        // окно появляется в системе не сразу — настраиваем его на следующем кадре
        Platform.runLater(() -> {
            hwnd = NativeWindows.find(title);
            NativeWindows.makeOverlay(hwnd);
        });
    }

    public void resize(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    /** Двигает окно, только если позиция реально изменилась — лишние вызовы дорогие. */
    public void moveTo(double x, double y) {
        double rx = Math.round(x);
        double ry = Math.round(y);
        if (rx != lastX) {
            stage.setX(rx);
            lastX = rx;
        }
        if (ry != lastY) {
            stage.setY(ry);
            lastY = ry;
        }
    }

    public void bringToTop() {
        NativeWindows.bringToTop(hwnd);
    }

    public void setVisible(boolean visible) {
        if (visible && !stage.isShowing()) {
            stage.show();
            Platform.runLater(() -> NativeWindows.makeOverlay(hwnd));
        } else if (!visible && stage.isShowing()) {
            stage.hide();
        }
    }

    public double stageX() {
        return lastX;
    }

    public double stageY() {
        return lastY;
    }

    public void close() {
        stage.close();
    }
}
