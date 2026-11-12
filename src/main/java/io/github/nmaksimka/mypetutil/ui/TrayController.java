package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Значок у часов (системный трей). Через него открываются настройки, когда окно закрыто.
 * Трей есть только в AWT, поэтому всё здесь выполняется в потоке AWT, а действия
 * передаются обратно в поток JavaFX через Platform.runLater.
 */
public final class TrayController {

    private static final Logger LOG = Logger.getLogger(TrayController.class.getName());

    public record Actions(Runnable openSettings, Consumer<PetCharacter> addPet, Runnable toggleHidden,
                          BooleanSupplier hidden, Runnable feedAll, Runnable exit) {
    }

    private TrayIcon icon;
    private MenuItem hideItem;

    public void install(Actions actions) {
        EventQueue.invokeLater(() -> {
            if (!SystemTray.isSupported()) {
                LOG.info("system tray is not supported");
                return;
            }
            try {
                SystemTray tray = SystemTray.getSystemTray();
                icon = new TrayIcon(trayImage(tray.getTrayIconSize()), "myPetUtil — пиксельные питомцы", menu(actions));
                icon.addActionListener(e -> Platform.runLater(actions.openSettings()));
                tray.add(icon);
            } catch (AWTException e) {
                LOG.log(Level.WARNING, "cannot add tray icon", e);
            }
        });
    }

    private PopupMenu menu(Actions actions) {
        Font font = new Font("Segoe UI", Font.PLAIN, 12);
        PopupMenu menu = new PopupMenu();
        menu.setFont(font);

        MenuItem settings = new MenuItem("Открыть настройки");
        settings.setFont(font.deriveFont(Font.BOLD));
        settings.addActionListener(e -> Platform.runLater(actions.openSettings()));

        Menu add = new Menu("Выпустить питомца");
        for (PetCharacter character : PetCharacter.values()) {
            MenuItem item = new MenuItem(character.title());
            item.addActionListener(e -> Platform.runLater(() -> actions.addPet().accept(character)));
            add.add(item);
        }

        MenuItem feed = new MenuItem("Покормить всех");
        feed.addActionListener(e -> Platform.runLater(actions.feedAll()));

        hideItem = new MenuItem("Спрятать питомцев");
        hideItem.addActionListener(e -> Platform.runLater(() -> {
            actions.toggleHidden().run();
            boolean hidden = actions.hidden().getAsBoolean();
            EventQueue.invokeLater(() -> hideItem.setLabel(hidden ? "Показать питомцев" : "Спрятать питомцев"));
        }));

        MenuItem exit = new MenuItem("Выход");
        exit.addActionListener(e -> Platform.runLater(actions.exit()));

        menu.add(settings);
        menu.addSeparator();
        menu.add(add);
        menu.add(feed);
        menu.add(hideItem);
        menu.addSeparator();
        menu.add(exit);
        return menu;
    }

    /** Пупс Клод, увеличенный под размер значка без размытия. */
    static BufferedImage trayImage(Dimension size) {
        PixelGrid grid = PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0));
        int w = Math.max(16, size.width);
        int h = Math.max(16, size.height);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int factor = Math.max(1, Math.min(w, h) / grid.width());
        int offsetX = (w - grid.width() * factor) / 2;
        int offsetY = (h - grid.height() * factor) / 2;
        for (int y = 0; y < grid.height() * factor; y++) {
            for (int x = 0; x < grid.width() * factor; x++) {
                image.setRGB(offsetX + x, offsetY + y, grid.get(x / factor, y / factor));
            }
        }
        return image;
    }

    public void setHiddenLabel(boolean hidden) {
        EventQueue.invokeLater(() -> {
            if (hideItem != null) {
                hideItem.setLabel(hidden ? "Показать питомцев" : "Спрятать питомцев");
            }
        });
    }

    public void notify(String title, String text) {
        EventQueue.invokeLater(() -> {
            if (icon != null) {
                icon.displayMessage(title, text, TrayIcon.MessageType.NONE);
            }
        });
    }

    public void remove() {
        EventQueue.invokeLater(() -> {
            if (icon != null) {
                SystemTray.getSystemTray().remove(icon);
                icon = null;
            }
        });
    }
}
