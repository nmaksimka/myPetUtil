package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.ThemeMode;
import io.github.nmaksimka.mypetutil.platform.NativeWindows;
import io.github.nmaksimka.mypetutil.platform.SystemTheme;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.util.List;
import java.util.Objects;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/** Окно настроек. Закрытие окна не выключает программу — она остаётся в трее. */
public final class SettingsWindow {

    public static final String TITLE = "myPetUtil — настройки";

    private final AppContext ctx;
    private final Stage stage = new Stage();
    private final BorderPane root = new BorderPane();
    private final ScrollPane scroll = new ScrollPane();
    private final List<Page> pages;
    private final SpritePreview logoPet = new SpritePreview(PetCharacter.CLAUDE, 3);
    private final Label onScreen = UiKit.label("", "side-footer");
    private final Button hideButton;
    private Page current;
    private boolean darkApplied;
    private boolean darkKnown;
    private double lastFooterUpdate;
    private double lastThemeCheck;
    private Runnable onFirstHide;
    private final List<ToggleButton> navButtons = new java.util.ArrayList<>();

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long nanos) {
            double now = nanos / 1e9;
            logoPet.tick(now);
            if (current != null) {
                current.tick(now);
            }
            if (now - lastFooterUpdate > 0.5) {
                lastFooterUpdate = now;
                updateFooter();
            }
            if (now - lastThemeCheck > 3) {
                lastThemeCheck = now;
                applyTheme();
            }
        }
    };

    public SettingsWindow(AppContext ctx) {
        this.ctx = ctx;
        pages = List.of(new PetsPage(ctx), new BehaviorPage(ctx), new FeedingPage(ctx), new AppsPage(ctx), new GeneralPage(ctx));

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(228);
        sidebar.setMinWidth(228);

        logoPet.setAnim(Anim.IDLE, 0);
        VBox logoTexts = new VBox(6, UiKit.label("myPetUtil", "logo-text"), UiKit.label("пиксельные питомцы", "logo-sub"));
        logoTexts.setAlignment(Pos.CENTER_LEFT);
        HBox logo = new HBox(10, logoPet, logoTexts);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setStyle("-fx-padding: 0 4 18 4;");
        logo.setOnMouseEntered(e -> logoPet.setAnim(Anim.HOVER, System.nanoTime() / 1e9));
        logo.setOnMouseExited(e -> logoPet.setAnim(Anim.IDLE, System.nanoTime() / 1e9));
        sidebar.getChildren().add(logo);

        ToggleGroup nav = new ToggleGroup();
        for (Page page : pages) {
            Rectangle marker = new Rectangle(6, 6);
            marker.getStyleClass().add("nav-marker");
            ToggleButton button = new ToggleButton(page.title(), marker);
            button.setGraphicTextGap(12);
            button.getStyleClass().setAll("nav-button");
            button.setToggleGroup(nav);
            button.setUserData(page);
            button.setMaxWidth(Double.MAX_VALUE);
            navButtons.add(button);
            sidebar.getChildren().add(button);
        }
        nav.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                nav.selectToggle(old);
                return;
            }
            open((Page) now.getUserData());
        });

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        hideButton = UiKit.button("Спрятать всех", () -> {
            ctx.pets().setUserHidden(!ctx.pets().isUserHidden());
            updateFooter();
        });
        hideButton.setMaxWidth(Double.MAX_VALUE);
        VBox footer = new VBox(10, onScreen, hideButton);
        sidebar.getChildren().addAll(grow, footer);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setLeft(sidebar);
        root.setCenter(scroll);

        Scene scene = new Scene(root, 980, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                SettingsWindow.class.getResource("/io/github/nmaksimka/mypetutil/ui/app.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle(TITLE);
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.getIcons().addAll(
                UiKit.scaled(PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0)), 2),
                UiKit.scaled(PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0)), 4));

        // крестик прячет окно, программа продолжает работать
        stage.setOnCloseRequest(e -> {
            e.consume();
            hide();
        });
        stage.setOnShown(e -> Platform.runLater(() -> {
            darkKnown = false;
            applyTheme();
        }));

        applyTheme();
        nav.selectToggle(nav.getToggles().get(0));
    }

    /** Открыть вкладку по номеру (0 — «Питомцы»). */
    public void openPage(int index) {
        navButtons.get(index).setSelected(true);
    }

    public int pageCount() {
        return pages.size();
    }

    private void open(Page page) {
        current = page;
        page.onShow();
        scroll.setContent(page.view());
        scroll.setVvalue(0);
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

    public boolean isShowing() {
        return stage.isShowing();
    }

    public Stage stage() {
        return stage;
    }

    /** Перекрашивает окно, если тема поменялась (в настройках или в самой Windows). */
    public void applyTheme() {
        ThemeMode mode = ctx.settings().theme;
        boolean dark = mode == ThemeMode.DARK || mode == ThemeMode.SYSTEM && SystemTheme.isDark();
        if (darkKnown && dark == darkApplied) {
            return;
        }
        darkKnown = true;
        darkApplied = dark;
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(dark ? "theme-dark" : "theme-light");
        if (stage.isShowing()) {
            NativeWindows.setDarkTitleBar(NativeWindows.find(TITLE), dark);
        }
    }

    /** Принудительно перечитать тему (вызывается после смены в настройках). */
    public void themeChanged() {
        darkKnown = false;
        applyTheme();
    }

    private void updateFooter() {
        int count = ctx.pets().pets().size();
        onScreen.setText(count == 0 ? "На экране никого" : "На экране: " + count + " " + plural(count));
        hideButton.setText(ctx.pets().isUserHidden() ? "Показать всех" : "Спрятать всех");
    }

    static String plural(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return "питомец";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "питомца";
        }
        return "питомцев";
    }
}
