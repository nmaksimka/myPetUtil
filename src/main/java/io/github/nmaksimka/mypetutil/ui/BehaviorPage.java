package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private final AppContext ctx;
    private final VBox root;
    private final StackPane sizePreviewBox = new StackPane();
    private SpritePreview sizePreview;
    private final Label sizeLabel = UiKit.label("", "muted", "small");

    BehaviorPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Поведение", "Изменения применяются сразу.");

        VBox modes = UiKit.group();
        List<HBox> options = new ArrayList<>();
        for (BehaviorMode mode : BehaviorMode.values()) {
            Circle dot = new Circle(6);
            dot.getStyleClass().add("radio-dot");
            HBox option = UiKit.row(mode.title(), mode.description(), null);
            option.getChildren().add(0, dot);
            option.getStyleClass().add("clickable");
            option.setUserData(mode);
            option.pseudoClassStateChanged(UiKit.SELECTED, s.mode == mode);
            option.setOnMouseClicked(e -> {
                s.mode = mode;
                options.forEach(o -> o.pseudoClassStateChanged(UiKit.SELECTED, o.getUserData() == mode));
                ctx.settingsChanged();
            });
            options.add(option);
            UiKit.addRow(modes, option);
        }

        Slider size = new Slider(Settings.MIN_SCALE, Settings.MAX_SCALE, s.scale);
        size.setMajorTickUnit(1);
        size.setMinorTickCount(0);
        size.setSnapToTicks(true);
        size.setBlockIncrement(1);
        size.valueProperty().addListener((obs, old, now) -> {
            int value = (int) Math.round(now.doubleValue());
            if (value != s.scale) {
                s.scale = value;
                updateSizePreview();
                ctx.settingsChanged();
            }
        });
        sizePreviewBox.setPrefSize(96, 96);
        sizePreviewBox.setMinSize(96, 96);
        sizePreviewBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox sizeTexts = new VBox(8, UiKit.label("Размер", "name"), size, sizeLabel);
        HBox.setHgrow(sizeTexts, Priority.ALWAYS);
        HBox sizeRow = new HBox(24, sizeTexts, sizePreviewBox);
        sizeRow.getStyleClass().add("row");
        updateSizePreview();

        Slider speed = new Slider(0.5, 2.0, s.speed);
        speed.setBlockIncrement(0.1);
        Label speedLabel = UiKit.label(speedText(s.speed), "muted", "small");
        speed.valueProperty().addListener((obs, old, now) -> {
            s.speed = Math.round(now.doubleValue() * 10) / 10.0;
            speedLabel.setText(speedText(s.speed));
            ctx.settingsChanged();
        });
        VBox speedRow = new VBox(8, UiKit.label("Скорость", "name"), speed, speedLabel);
        speedRow.getStyleClass().add("row");

        Switch monitors = new Switch(s.multiMonitor);
        monitors.selectedProperty().addListener((obs, old, now) -> {
            s.multiMonitor = now;
            ctx.settingsChanged();
        });

        root.getChildren().addAll(
                UiKit.heading("Режим"), modes,
                UiKit.heading("Внешний вид"), UiKit.group(sizeRow, speedRow),
                UiKit.heading("Мониторы"),
                UiKit.group(UiKit.row("Переходить между мониторами",
                        "Уходит за край одного монитора и появляется с любой стороны другого.", monitors)));
    }

    private void updateSizePreview() {
        int scale = ctx.settings().scale;
        sizePreview = new SpritePreview(PetCharacter.CLAUDE, Math.min(scale, 6));
        sizePreview.setAnim(Anim.WALK, System.nanoTime() / 1e9);
        sizePreviewBox.getChildren().setAll(sizePreview);
        String name = switch (scale) {
            case 2 -> "Крошечный";
            case 3 -> "Маленький";
            case 4 -> "Средний";
            case 5 -> "Большой";
            default -> "Огромный";
        };
        sizeLabel.setText(name + ", " + 16 * scale + " точек");
    }

    private static String speedText(double speed) {
        String word = speed < 0.8 ? "прогулочный шаг" : speed > 1.4 ? "бегом" : "обычный шаг";
        return String.format(Locale.ROOT, "%.1fx, ", speed) + word;
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
