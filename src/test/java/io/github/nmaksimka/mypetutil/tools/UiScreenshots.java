package io.github.nmaksimka.mypetutil.tools;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.config.Theme;
import io.github.nmaksimka.mypetutil.overlay.PetManager;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.PhysRect;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;
import io.github.nmaksimka.mypetutil.platform.WindowScanner;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.ui.AppContext;
import io.github.nmaksimka.mypetutil.ui.SettingsWindow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/**
 * Снимает все вкладки окна настроек в светлой и тёмной теме в docs/screenshots.
 * Запуск: gradlew uiScreenshots. Рабочий стол не фотографируется — только само окно.
 */
public class UiScreenshots extends Application implements AppContext {

    private static final String[] NAMES = {"pets", "behavior", "food", "apps", "themes", "system"};

    private final Settings settings = new Settings();
    private PetManager manager;
    private WindowScanner scanner;
    private SettingsWindow window;
    private Path out;
    private int step;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        out = Path.of(getParameters().getRaw().isEmpty() ? "docs/screenshots" : getParameters().getRaw().get(0));
        Files.createDirectories(out);
        settings.mode = BehaviorMode.WINDOWS;
        settings.hungerEnabled = true;
        settings.theme = Theme.SNOW;
        settings.blockedApps.add("game.exe");
        double[] satiety = {92, 64, 18};
        PetCharacter[] who = {PetCharacter.CLAUDE, PetCharacter.SAKURA, PetCharacter.STEVE};
        for (int i = 0; i < who.length; i++) {
            PetEntry entry = new PetEntry(who[i]);
            entry.satiety = satiety[i];
            settings.pets.add(entry);
        }
        scanner = new WindowScanner();
        scanner.start();
        manager = new PetManager(settings, scanner, () -> { });
        manager.setUserHidden(true);
        manager.start();
        window = new SettingsWindow(this);
        window.show();
        next();
    }

    /** Шаги по очереди: переключить вкладку → подождать отрисовку → сохранить. */
    private void next() {
        int pages = window.pageCount();
        if (step >= pages * 2) {
            Platform.exit();
            System.exit(0);
            return;
        }
        boolean dark = step >= pages;
        settings.theme = dark ? Theme.GRAPHITE : Theme.SNOW;
        window.themeChanged();
        window.openPage(step % pages);
        PauseTransition wait = new PauseTransition(Duration.millis(900));
        wait.setOnFinished(e -> {
            save(window.stage().getScene().snapshot(null), NAMES[step % pages] + (dark ? "-dark" : "") + ".png");
            step++;
            next();
        });
        wait.play();
    }

    private void save(WritableImage image, String name) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(buffered, "png", out.resolve(name).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public PetManager pets() {
        return manager;
    }

    @Override
    public void settingsChanged() {
    }

    @Override
    public void themeChanged() {
        window.themeChanged();
    }

    @Override
    public Path settingsFile() {
        return Path.of("C:\\Users\\user\\AppData\\Roaming\\myPetUtil\\settings.json");
    }

    /** Выдуманные окна, чтобы на скриншоты не попали настоящие заголовки. */
    @Override
    public List<RawWindow> openWindows() {
        PhysRect r = new PhysRect(0, 0, 800, 600);
        return List.of(
                new RawWindow(1, r, "chrome.exe", "Новая вкладка — Google Chrome"),
                new RawWindow(2, r, "idea64.exe", "myPetUtil – PetManager.java"),
                new RawWindow(3, r, "telegram.exe", "Telegram"),
                new RawWindow(4, r, "explorer.exe", "Загрузки"),
                new RawWindow(5, r, "spotify.exe", "Spotify Premium"));
    }

    @Override
    public void exit() {
        Platform.exit();
    }
}
