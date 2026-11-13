package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.Theme;
import io.github.nmaksimka.mypetutil.config.Theme.Palette;
import io.github.nmaksimka.mypetutil.platform.SystemTheme;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Вкладка «Темы»: плитки с маленьким макетом окна в цветах каждой темы. */
final class ThemesPage implements Page {

    private final VBox root;

    ThemesPage(AppContext ctx) {
        root = UiKit.page("Темы", "Как выглядит это окно. Питомцы от темы не меняются.");

        FlowPane tiles = new FlowPane(20, 20);
        List<VBox> all = new ArrayList<>();
        for (Theme theme : Theme.values()) {
            VBox tile = new VBox(sample(theme), UiKit.label(theme.title(), "theme-name"));
            tile.getStyleClass().add("theme-tile");
            tile.setUserData(theme);
            tile.pseudoClassStateChanged(UiKit.SELECTED, ctx.settings().theme == theme);
            tile.setOnMouseClicked(e -> {
                ctx.settings().theme = theme;
                all.forEach(t -> t.pseudoClassStateChanged(UiKit.SELECTED, t.getUserData() == theme));
                ctx.settingsChanged();
                ctx.themeChanged();
            });
            all.add(tile);
            tiles.getChildren().add(tile);
        }
        root.getChildren().add(tiles);
    }

    /** Мини-окно: шапка с подчёркнутой вкладкой, строчки текста и акцентная кнопка. */
    private static Node sample(Theme theme) {
        if (theme == Theme.SYSTEM) {
            // половина светлая, половина тёмная
            HBox split = new HBox(miniWindow(Theme.SNOW.palette(false), 75), miniWindow(Theme.GRAPHITE.palette(true), 75));
            return frame(split);
        }
        return frame(miniWindow(theme.palette(SystemTheme.isDark()), 150));
    }

    private static Node frame(Node content) {
        StackPane frame = new StackPane(content);
        frame.getStyleClass().add("theme-sample");
        frame.setPadding(new javafx.geometry.Insets(3));
        return frame;
    }

    private static Node miniWindow(Palette p, double width) {
        VBox window = new VBox(7);
        window.setPrefSize(width, 96);
        window.setMinSize(width, 96);
        window.setMaxSize(width, 96);
        window.setStyle("-fx-background-color: " + p.bg() + "; -fx-padding: 10;");

        HBox tabs = new HBox(6, bar(p.text(), 18, 3), bar(p.muted(), 14, 3));
        Region underline = bar(p.accent(), 18, 2);
        VBox header = new VBox(3, tabs, underline);

        VBox lines = new VBox(5, bar(p.text(), width * 0.55, 4), bar(p.muted(), width * 0.4, 3));
        lines.setStyle("-fx-background-color: " + p.surface() + "; -fx-border-color: " + p.line()
                + "; -fx-padding: 6; -fx-background-radius: 3; -fx-border-radius: 3;");

        Region button = bar(p.accent(), 26, 8);
        button.setStyle(button.getStyle() + "-fx-background-radius: 2;");
        HBox buttonRow = new HBox(button);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        window.getChildren().addAll(header, lines, buttonRow);
        return window;
    }

    private static Region bar(String color, double width, double height) {
        Region bar = new Region();
        bar.setPrefSize(width, height);
        bar.setMinSize(width, height);
        bar.setMaxSize(width, height);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 1;");
        return bar;
    }

    @Override
    public String title() {
        return "Темы";
    }

    @Override
    public Node view() {
        return root;
    }
}
