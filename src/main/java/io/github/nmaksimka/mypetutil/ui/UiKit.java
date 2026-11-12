package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.sprite.PixelGrid;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Маленькие фабрики элементов, чтобы страницы выглядели одинаково. */
public final class UiKit {

    private UiKit() {
    }

    public static Label label(String text, String... styles) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styles);
        return label;
    }

    public static Label text(String text, String... styles) {
        Label label = label(text, styles);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().setAll("btn");
        button.setOnAction(e -> action.run());
        return button;
    }

    public static Button primary(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("btn-primary");
        return button;
    }

    public static Button flat(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().setAll("btn-flat");
        button.setOnAction(e -> action.run());
        return button;
    }

    public static Button danger(String text, Runnable action) {
        Button button = flat(text, action);
        button.getStyleClass().add("btn-danger");
        return button;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    /** Страница: заголовок пиксельным шрифтом и пояснение под ним. */
    public static VBox page(String title, String subtitle) {
        VBox page = new VBox();
        page.getStyleClass().add("page");
        VBox header = new VBox(10, label(title, "page-title"), text(subtitle, "page-subtitle"));
        page.getChildren().add(header);
        return page;
    }

    public static Label section(String title) {
        return label(title.toUpperCase(), "section-title");
    }

    /** Строка настройки: слева название и описание, справа элемент управления. */
    public static HBox settingRow(String title, String description, Node control) {
        VBox texts = new VBox(3, label(title, "strong"));
        if (description != null) {
            texts.getChildren().add(text(description, "muted", "small"));
        }
        HBox.setHgrow(texts, Priority.ALWAYS);
        texts.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(16, texts, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        return row;
    }

    /** Несколько кнопок, из которых выбрана ровно одна. */
    public static <T> HBox segmented(List<T> values, Function<T, String> title, T selected, Consumer<T> onSelect) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup group = new ToggleGroup();
        for (T value : values) {
            ToggleButton button = new ToggleButton(title.apply(value));
            button.getStyleClass().setAll("segment");
            button.setToggleGroup(group);
            button.setUserData(value);
            button.setSelected(value.equals(selected));
            box.getChildren().add(button);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                // выбор нельзя снять совсем — возвращаем старый
                group.selectToggle(old);
                return;
            }
            @SuppressWarnings("unchecked")
            T value = (T) now.getUserData();
            onSelect.accept(value);
        });
        return box;
    }

    /** Картинка из пиксельной сетки, увеличенная без размытия — для иконок окна и трея. */
    public static Image scaled(PixelGrid grid, int factor) {
        int w = grid.width() * factor;
        int h = grid.height() * factor;
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                argb[y * w + x] = grid.get(x / factor, y / factor);
            }
        }
        WritableImage image = new WritableImage(w, h);
        image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        return image;
    }
}
