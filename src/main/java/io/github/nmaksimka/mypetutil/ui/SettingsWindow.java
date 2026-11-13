package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.Theme;
import io.github.nmaksimka.mypetutil.platform.NativeWindows;
import io.github.nmaksimka.mypetutil.platform.SystemTheme;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** Окно настроек. Крестик прячет окно, программа остаётся в трее. */
public final class SettingsWindow {

    public static final String TITLE = "myPetUtil";

    private final AppContext ctx;
    private final Stage stage = new Stage();
    private final BorderPane root = new BorderPane();
    private final ScrollPane scroll = new ScrollPane();
    private final List<Page> pages;
    private final List<ToggleButton> tabs = new ArrayList<>();
    private final Button hideButton;
    private Page current;
    private String appliedCss;
    private double lastThemeCheck;
    private double lastHeaderUpdate;
    private Runnable onFirstHide;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long nanos) {
            double now = nanos / 1e9;
            if (current != null) {
                current.tick(now);
            }
            if (now - lastHeaderUpdate > 0.5) {
                lastHeaderUpdate = now;
                hideButton.setText(ctx.pets().isUserHidden() ? "Показать питомцев" : "Спрятать питомцев");
            }
            if (now - lastThemeCheck > 3) {
                lastThemeCheck = now;
                applyTheme();
            }
        }
    };

    public SettingsWindow(AppContext ctx) {
        this.ctx = ctx;
        pages = List.of(new PetsPage(ctx), new BehaviorPage(ctx), new FeedingPage(ctx),
                new AppsPage(ctx), new ThemesPage(ctx), new GeneralPage(ctx));

        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.getChildren().add(UiKit.label("myPetUtil", "app-name"));
        ToggleGroup group = new ToggleGroup();
        HBox tabRow = new HBox(22);
        tabRow.setAlignment(Pos.BOTTOM_LEFT);
        for (Page page : pages) {
            ToggleButton tab = new ToggleButton(page.title());
            tab.getStyleClass().setAll("tab");
            tab.setToggleGroup(group);
            tab.setUserData(page);
            tabs.add(tab);
            tabRow.getChildren().add(tab);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                group.selectToggle(old);
                return;
            }
            open((Page) now.getUserData());
        });
        hideButton = UiKit.quiet("Спрятать питомцев", () -> ctx.pets().setUserHidden(!ctx.pets().isUserHidden()));
        HBox hideBox = new HBox(hideButton);
        hideBox.setStyle("-fx-padding: 0 0 8 0;");
        header.getChildren().addAll(tabRow, UiKit.spacer(), hideBox);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setTop(header);
        root.setCenter(scroll);

        Scene scene = new Scene(root, 900, 640);
        scene.getStylesheets().add(Objects.requireNonNull(
                SettingsWindow.class.getResource("/io/github/nmaksimka/mypetutil/ui/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle(TITLE);
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        stage.getIcons().addAll(
                UiKit.scaled(PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0)), 2),
                UiKit.scaled(PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0)), 4));

        stage.setOnCloseRequest(e -> {
            e.consume();
            hide();
        });
        stage.setOnShown(e -> Platform.runLater(this::themeChanged));

        applyTheme();
        tabs.get(0).setSelected(true);
    }

    private void open(Page page) {
        current = page;
        page.onShow();
        scroll.setContent(page.view());
        scroll.setVvalue(0);
    }

    /** Открыть вкладку по номеру (0 — «Питомцы»). */
    public void openPage(int index) {
        tabs.get(index).setSelected(true);
    }

    public int pageCount() {
        return pages.size();
    }

    public void show() {
        if (current != null) {
            current.onShow();
        }
        stage.show();
        if (stage.isIconified()) {
            stage.setIconified(false);
        }
        stage.toFront();
        stage.requestFocus();
        timer.start();
    }

    public void hide() {
        stage.hide();
        timer.stop();
        if (onFirstHide != null) {
            Runnable once = onFirstHide;
            onFirstHide = null;
            once.run();
        }
    }

    /** Что сделать, когда окно впервые закроют (показать подсказку про трей). */
    public void setOnFirstHide(Runnable action) {
        this.onFirstHide = action;
    }

    public Stage stage() {
        return stage;
    }

    /** Подставляет цвета темы. Тема «Как в Windows» сама следит за системной. */
    private void applyTheme() {
        Theme theme = ctx.settings().theme;
        Theme.Palette palette = theme.palette(theme == Theme.SYSTEM && SystemTheme.isDark());
        String css = palette.toCss();
        if (css.equals(appliedCss)) {
            return;
        }
        appliedCss = css;
        root.setStyle(css);
        if (stage.isShowing()) {
            NativeWindows.setDarkTitleBar(NativeWindows.find(TITLE), palette.dark());
        }
    }

    /** Перечитать тему (после смены в настройках). */
    public void themeChanged() {
        appliedCss = null;
        applyTheme();
    }
}
