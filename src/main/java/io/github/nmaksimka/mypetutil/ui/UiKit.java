package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.sprite.PixelGrid;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.css.PseudoClass;
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

/** Маленькие фабрики элементов, чтобы все вкладки выглядели одинаково. */
public final class UiKit {

    public static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private UiKit() {
    }

    public static Label label(String text, String... styles) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styles);
        return label;
    }

    /** Текст, который переносится на новые строки. */
    public static Label text(String text, String... styles) {
        Label label = label(text, styles);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /** Главная кнопка (залита акцентным цветом). */
    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().setAll("btn");
        button.setOnAction(e -> action.run());
        return button;
    }

    /** Тихая кнопка-ссылка. */
    public static Button quiet(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().setAll("btn-quiet");
        button.setOnAction(e -> action.run());
        return button;
    }

    /** Тихая кнопка для удаления: серая, чтобы не кричать. */
    public static Button quietDanger(String text, Runnable action) {
        Button button = quiet(text, action);
        button.getStyleClass().add("danger");
        return button;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    /** Страница: заголовок и короткое пояснение. */
    public static VBox page(String title, String lead) {
        VBox page = new VBox();
        page.getStyleClass().add("page");
        VBox header = new VBox(4, label(title, "title"));
        if (lead != null) {
            header.getChildren().add(text(lead, "lead"));
        }
        page.getChildren().add(header);
        return page;
    }

    public static Label heading(String title) {
        return label(title, "heading");
    }

    /** Группа строк в одной рамке, между строками тонкие линии. */
    public static VBox group(Node... rows) {
        VBox group = new VBox();
        group.getStyleClass().add("group");
        for (Node row : rows) {
            addRow(group, row);
        }
        return group;
    }

    public static void addRow(VBox group, Node row) {
        if (group.getChildren().isEmpty()) {
            row.getStyleClass().add("first");
        }
        group.getChildren().add(row);
    }

    /** Строка настройки: слева название и пояснение, справа элемент управления. */
    public static HBox row(String title, String description, Node control) {
        VBox texts = new VBox(2, label(title, "name"));
        if (description != null) {
            texts.getChildren().add(text(description, "muted", "small"));
        }
        HBox.setHgrow(texts, Priority.ALWAYS);
        texts.setMaxWidth(Double.MAX_VALUE);
        HBox row = control == null ? new HBox(texts) : new HBox(texts, control);
        row.getStyleClass().add("row");
        return row;
    }

    /** Несколько вариантов в одной плашке, выбран ровно один. */
    public static <T> HBox segmented(List<T> values, Function<T, String> title, T selected, Consumer<T> onSelect) {
        HBox box = new HBox();
        box.getStyleClass().add("segments");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Region.USE_PREF_SIZE);
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
                group.selectToggle(old);
                return;
            }
            @SuppressWarnings("unchecked")
            T value = (T) now.getUserData();
            onSelect.accept(value);
        });
        return box;
    }

    /** Картинка из пиксельной сетки, увеличенная без размытия — для иконки окна. */
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
