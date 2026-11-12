package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Вкладка «Поведение»: режим, размер, скорость, мониторы. */
final class BehaviorPage implements Page {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final AppContext ctx;
    private final VBox root;
    private final StackPane sizePreviewBox = new StackPane();
    private SpritePreview sizePreview;
    private final Label sizeLabel = UiKit.label("", "muted");

    BehaviorPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Поведение", "Как питомцы ведут себя на экране. Всё применяется сразу.");

        VBox modes = new VBox(10);
        List<HBox> options = new ArrayList<>();
        for (BehaviorMode mode : BehaviorMode.values()) {
            Circle dot = new Circle(6);
            dot.getStyleClass().add("radio-dot");
            VBox texts = new VBox(3, UiKit.label(mode.title(), "strong"), UiKit.text(mode.description(), "muted", "small"));
            HBox.setHgrow(texts, Priority.ALWAYS);
            HBox option = new HBox(14, dot, texts);
            option.setAlignment(Pos.CENTER_LEFT);
            option.getStyleClass().add("mode-option");
            option.setUserData(mode);
            option.pseudoClassStateChanged(SELECTED, s.mode == mode);
            option.setOnMouseClicked(e -> {
                s.mode = mode;
                options.forEach(o -> o.pseudoClassStateChanged(SELECTED, o.getUserData() == mode));
                ctx.settingsChanged();
            });
            options.add(option);
            modes.getChildren().add(option);
        }

        Slider size = new Slider(Settings.MIN_SCALE, Settings.MAX_SCALE, s.scale);
        size.setMajorTickUnit(1);
        size.setMinorTickCount(0);
        size.setSnapToTicks(true);
        size.setBlockIncrement(1);
        HBox.setHgrow(size, Priority.ALWAYS);
        size.valueProperty().addListener((obs, old, now) -> {
            int value = (int) Math.round(now.doubleValue());
            if (value != s.scale) {
                s.scale = value;
                updateSizePreview();
                ctx.settingsChanged();
            }
        });
        sizePreviewBox.setPrefSize(100, 100);
        sizePreviewBox.setMinSize(100, 100);
        sizePreviewBox.setAlignment(Pos.BOTTOM_CENTER);
        sizePreviewBox.getStyleClass().add("preview-box");
        VBox sizeTexts = new VBox(8, UiKit.label("Размер питомца", "strong"), size, sizeLabel);
        HBox.setHgrow(sizeTexts, Priority.ALWAYS);
        HBox sizeRow = new HBox(20, sizeTexts, sizePreviewBox);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        sizeRow.getStyleClass().add("card");
        updateSizePreview();

        Slider speed = new Slider(0.5, 2.0, s.speed);
        speed.setBlockIncrement(0.1);
        Label speedLabel = UiKit.label(speedText(s.speed), "muted");
        speed.valueProperty().addListener((obs, old, now) -> {
            s.speed = Math.round(now.doubleValue() * 10) / 10.0;
            speedLabel.setText(speedText(s.speed));
            ctx.settingsChanged();
        });
        VBox speedBox = new VBox(8, UiKit.label("Скорость ходьбы", "strong"), speed, speedLabel);
        speedBox.getStyleClass().add("card");

        Switch monitors = new Switch(s.multiMonitor);
        monitors.selectedProperty().addListener((obs, old, now) -> {
            s.multiMonitor = now;
            ctx.settingsChanged();
        });

        root.getChildren().addAll(
                UiKit.section("Режим"), modes,
                UiKit.section("Внешний вид"), sizeRow, speedBox,
                UiKit.section("Несколько мониторов"),
                UiKit.settingRow("Переходить между мониторами",
                        "Питомец уходит за край одного монитора и появляется с любой стороны другого.", monitors));
    }

    private void updateSizePreview() {
        int scale = ctx.settings().scale;
        // в превью размер честный, но не больше рамки
        double pixel = Math.min(scale, 6);
        sizePreview = new SpritePreview(PetCharacter.CLAUDE, pixel);
        sizePreview.setAnim(Anim.WALK, System.nanoTime() / 1e9);
        sizePreviewBox.getChildren().setAll(sizePreview);
        int px = 16 * scale;
        String name = scale <= 2 ? "Крошечный" : scale == 3 ? "Маленький" : scale == 4 ? "Средний" : scale == 5 ? "Большой" : "Огромный";
        sizeLabel.setText(name + " · " + px + "×" + px + " точек");
    }

    private static String speedText(double speed) {
        return String.format(Locale.ROOT, "×%.1f", speed) + (speed < 0.8 ? " · прогулочным шагом" : speed > 1.4 ? " · бегом" : " · обычный шаг");
    }

    @Override
    public String title() {
        return "Поведение";
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void tick(double now) {
        if (sizePreview != null) {
            sizePreview.tick(now);
        }
    }
}
