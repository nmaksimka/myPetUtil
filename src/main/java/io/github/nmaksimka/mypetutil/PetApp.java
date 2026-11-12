package io.github.nmaksimka.mypetutil;

import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.config.SettingsStore;
import io.github.nmaksimka.mypetutil.overlay.PetManager;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.platform.WindowScanner;
import io.github.nmaksimka.mypetutil.ui.AppContext;
import io.github.nmaksimka.mypetutil.ui.SettingsWindow;
import io.github.nmaksimka.mypetutil.ui.TrayController;

import java.io.InputStream;
import java.nio.file.Path;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Приложение: питомцы на экране, окно настроек и значок в трее.
 * Флаг --tray (так запускает автозагрузка) — стартовать без окна настроек.
 */
public class PetApp extends Application implements AppContext {

    private SettingsStore store;
    private Settings settings;
    private WindowScanner scanner;
    private PetManager manager;
    private SettingsWindow settingsWindow;
    private final TrayController tray = new TrayController();
    private final PauseTransition saveDelay = new PauseTransition(Duration.millis(400));

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        if (!SingleInstance.acquire(SettingsStore.defaultDirectory(), () -> Platform.runLater(this::openSettings))) {
            Platform.exit();
            return;
        }
        loadFont();

        store = SettingsStore.createDefault();
        settings = store.load();
        saveDelay.setOnFinished(e -> store.save(settings));

        scanner = new WindowScanner();
        scanner.start();
        manager = new PetManager(settings, scanner, this::settingsChanged);
        manager.start();

        settingsWindow = new SettingsWindow(this);
        settingsWindow.setOnFirstHide(() -> tray.notify("myPetUtil работает в трее",
                "Питомцы остаются на экране. Настройки — двойной клик по значку у часов."));
        tray.install(new TrayController.Actions(
                this::openSettings,
                manager::addPet,
                () -> {
                    manager.setUserHidden(!manager.isUserHidden());
                    tray.setHiddenLabel(manager.isUserHidden());
                },
                manager::isUserHidden,
                () -> manager.pets().stream().map(Pet::entry).forEach(entry -> manager.dropFoodNear(entry.id)),
                this::exit));

        boolean startInTray = getParameters().getRaw().contains("--tray");
        if (!startInTray) {
            settingsWindow.show();
        }
    }

    private static void loadFont() {
        try (InputStream font = PetApp.class.getResourceAsStream("/io/github/nmaksimka/mypetutil/fonts/PressStart2P-Regular.ttf")) {
            if (font != null) {
                Font.loadFont(font, 12);
            }
        } catch (Exception ignored) {
            // без пиксельного шрифта заголовки просто будут обычным шрифтом
        }
    }

    private void openSettings() {
        if (settingsWindow != null) {
            settingsWindow.show();
        }
    }

    // ---------- AppContext ----------

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public PetManager pets() {
        return manager;
    }

    /** Сохраняем через 0.4 с после последнего изменения, чтобы не писать файл на каждое движение слайдера. */
    @Override
    public void settingsChanged() {
        saveDelay.playFromStart();
    }

    @Override
    public void themeChanged() {
        if (settingsWindow != null) {
            settingsWindow.themeChanged();
        }
    }

    @Override
    public Path settingsFile() {
        return store.file();
    }

    @Override
    public void exit() {
        Platform.exit();
    }

    @Override
    public void stop() {
        if (manager != null) {
            manager.stop();
        }
        if (scanner != null) {
            scanner.close();
        }
        if (store != null) {
            store.save(settings);
        }
        tray.remove();
        SingleInstance.release();
        // поток AWT (трей) сам не завершается
        new Thread(() -> System.exit(0), "exit").start();
    }
}
