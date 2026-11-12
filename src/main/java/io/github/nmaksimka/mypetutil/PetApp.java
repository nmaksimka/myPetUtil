package io.github.nmaksimka.mypetutil;

import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.config.SettingsStore;
import io.github.nmaksimka.mypetutil.overlay.PetManager;
import io.github.nmaksimka.mypetutil.platform.WindowScanner;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PetApp extends Application {

    private SettingsStore store;
    private Settings settings;
    private WindowScanner scanner;
    private PetManager manager;
    private final PauseTransition saveDelay = new PauseTransition(Duration.millis(400));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        store = SettingsStore.createDefault();
        settings = store.load();
        saveDelay.setOnFinished(e -> store.save(settings));

        scanner = new WindowScanner();
        scanner.start();
        manager = new PetManager(settings, scanner, this::requestSave);
        manager.start();
    }

    /** Сохраняем не сразу, а через 0.4 с после последнего изменения — чтобы не писать файл на каждый щелчок слайдера. */
    private void requestSave() {
        saveDelay.playFromStart();
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
    }
}
