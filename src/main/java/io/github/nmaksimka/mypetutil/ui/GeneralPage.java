package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.platform.Autostart;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Вкладка «Система»: автозапуск, папка с настройками, версия, выход. */
final class GeneralPage implements Page {

    private static final Logger LOG = Logger.getLogger(GeneralPage.class.getName());
    static final String VERSION = "1.0.0";

    private final AppContext ctx;
    private final VBox root;
    private final Switch autostart;
    private final HBox autostartRow;

    GeneralPage(AppContext ctx) {
        this.ctx = ctx;
        root = UiKit.page("Система", null);

        autostart = new Switch(Autostart.isEnabled());
        autostart.setDisable(!Autostart.available());
        autostart.selectedProperty().addListener((obs, old, now) -> {
            if (Autostart.setEnabled(now)) {
                ctx.settings().autostart = now;
                ctx.settingsChanged();
            } else if (now) {
                autostart.setSelected(false);
            }
        });
        autostartRow = UiKit.row("Запускать вместе с Windows", autostartHint(), autostart);

        HBox folder = UiKit.row("Папка с настройками", ctx.settingsFile().getParent().toString(),
                UiKit.quiet("Открыть", this::openFolder));

        root.getChildren().addAll(
                UiKit.group(autostartRow, folder),
                UiKit.heading("О программе"),
                UiKit.group(
                        UiKit.row("myPetUtil " + VERSION,
                                "Пиксельные питомцы для рабочего стола. Java 21 и JavaFX. Автор: nmaksimka", null),
                        UiKit.row("Выйти из программы", "Питомцы исчезнут до следующего запуска.",
                                UiKit.quietDanger("Выйти", ctx::exit))));
    }

    private static String autostartHint() {
        return Autostart.available()
                ? "Программа стартует без окна, с питомцами и значком у часов."
                : "Работает в собранной версии myPetUtil.exe. При запуске из IDE недоступно.";
    }

    private void openFolder() {
        try {
            new ProcessBuilder("explorer.exe", ctx.settingsFile().getParent().toString()).start();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot open folder", e);
        }
    }

    @Override
    public String title() {
        return "Система";
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        autostart.setSelected(Autostart.isEnabled());
    }
}
