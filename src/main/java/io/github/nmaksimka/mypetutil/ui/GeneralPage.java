package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.config.ThemeMode;
import io.github.nmaksimka.mypetutil.platform.Autostart;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Вкладка «Общее»: тема, автозапуск, где лежат настройки, о программе. */
final class GeneralPage implements Page {

    private static final Logger LOG = Logger.getLogger(GeneralPage.class.getName());
    static final String VERSION = "1.0.0";

    private final AppContext ctx;
    private final VBox root;
    private final Switch autostart;
    private final Label autostartHint;

    GeneralPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Общее", "Оформление, запуск вместе с Windows и всё остальное.");

        HBox theme = UiKit.segmented(List.of(ThemeMode.values()), ThemeMode::title, s.theme, value -> {
            s.theme = value;
            ctx.settingsChanged();
            ctx.themeChanged();
        });
        VBox themeBox = new VBox(10, UiKit.label("Тема окна", "strong"),
                UiKit.text("«Как в Windows» — переключается сама вместе с темой системы.", "muted", "small"), theme);
        themeBox.getStyleClass().add("card");

        autostart = new Switch(Autostart.isEnabled());
        autostart.setDisable(!Autostart.available());
        autostartHint = UiKit.text("", "muted", "small");
        autostart.selectedProperty().addListener((obs, old, now) -> {
            if (Autostart.setEnabled(now)) {
                s.autostart = now;
                ctx.settingsChanged();
            } else if (now) {
                autostart.setSelected(false);
            }
            updateAutostartHint();
        });
        updateAutostartHint();
        VBox autostartTexts = new VBox(3, UiKit.label("Запускать вместе с Windows", "strong"),
                UiKit.text("По умолчанию выключено. При автозапуске окно настроек не открывается — "
                        + "питомцы просто появляются, а программа ждёт в трее.", "muted", "small"),
                autostartHint);
        javafx.scene.layout.HBox.setHgrow(autostartTexts, javafx.scene.layout.Priority.ALWAYS);
        HBox autostartRow = new HBox(16, autostartTexts, autostart);
        autostartRow.setAlignment(Pos.CENTER_LEFT);
        autostartRow.getStyleClass().add("card");

        Label path = UiKit.text(ctx.settingsFile().toString(), "muted", "small");
        VBox files = new VBox(8, UiKit.label("Настройки и питомцы хранятся здесь", "strong"), path,
                UiKit.button("Открыть папку", this::openFolder));
        files.getStyleClass().add("card");

        VBox about = new VBox(6,
                UiKit.label("myPetUtil " + VERSION, "strong"),
                UiKit.text("Пиксельные питомцы для рабочего стола: гуляют по панели задач и окнам, "
                        + "едят, радуются и грустят. Написано на Java 21 и JavaFX.", "muted", "small"),
                UiKit.text("Автор: nmaksimka · github.com/nmaksimka", "muted", "small"));
        about.getStyleClass().add("card");

        HBox exitRow = new HBox(UiKit.spacer(), UiKit.button("Закрыть программу", ctx::exit));

        root.getChildren().addAll(
                UiKit.section("Оформление"), themeBox,
                UiKit.section("Запуск"), autostartRow,
                UiKit.section("Файлы"), files,
                UiKit.section("О программе"), about, exitRow);
    }

    private void updateAutostartHint() {
        if (!Autostart.available()) {
            autostartHint.setText("Доступно в собранной версии (myPetUtil.exe). При запуске из IDE exe-файла нет.");
        } else {
            autostartHint.setText(autostart.isSelected() ? "Включено: программа есть в автозагрузке." : "");
        }
        autostartHint.setVisible(!autostartHint.getText().isEmpty());
        autostartHint.setManaged(autostartHint.isVisible());
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
        return "Общее";
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        autostart.setSelected(Autostart.isEnabled());
        updateAutostartHint();
    }
}
