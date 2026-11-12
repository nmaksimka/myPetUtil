package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;

import java.util.Map;
import java.util.TreeMap;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Вкладка «Программы»: по окнам каких программ можно гулять. */
final class AppsPage implements Page {

    private final AppContext ctx;
    private final VBox root;
    private final VBox list = new VBox();
    private final Label modeBanner;

    AppsPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Программы",
                "Сначала питомцам можно гулять везде. Снимите переключатель у программы — "
                        + "и они перестанут залезать на её окна.");

        modeBanner = UiKit.text("Сейчас выбран другой режим поведения — окна программ используются только в режиме "
                + "«" + BehaviorMode.WINDOWS.title() + "». Его можно включить на вкладке «Поведение».", "banner");

        Switch hide = new Switch(s.hideOnBlockedFullscreen);
        hide.selectedProperty().addListener((obs, old, now) -> {
            s.hideOnBlockedFullscreen = now;
            ctx.settingsChanged();
        });

        list.getStyleClass().add("list");
        HBox listHeader = new HBox(UiKit.section("Открытые программы"), UiKit.spacer(),
                UiKit.flat("Обновить список", this::rebuild));
        listHeader.setAlignment(Pos.BOTTOM_LEFT);

        root.getChildren().addAll(modeBanner,
                UiKit.settingRow("Прятаться от запрещённых игр",
                        "Если запрещённая программа развёрнута на весь экран (например, игра), питомцы "
                                + "прячутся и возвращаются, когда вы из неё выйдете.", hide),
                listHeader, list,
                UiKit.text("В полноэкранных программах, которые не запрещены, питомцы гуляют по самому низу монитора.",
                        "muted", "small"));
    }

    private void rebuild() {
        Settings s = ctx.settings();
        modeBanner.setVisible(s.mode != BehaviorMode.WINDOWS);
        modeBanner.setManaged(s.mode != BehaviorMode.WINDOWS);

        // exe -> пример заголовка окна; запрещённые показываем, даже если они сейчас закрыты
        Map<String, String> apps = new TreeMap<>();
        for (RawWindow window : ctx.openWindows()) {
            if (!window.exe().equals("mypetutil.exe") && !window.exe().equals("unknown")) {
                apps.putIfAbsent(window.exe(), window.title());
            }
        }
        for (String blocked : s.blockedApps) {
            apps.putIfAbsent(blocked, "сейчас не запущена");
        }

        list.getChildren().clear();
        if (apps.isEmpty()) {
            list.getChildren().add(UiKit.text("Открытых окон не нашлось. Откройте программу и нажмите «Обновить список».", "empty"));
            return;
        }
        int index = 0;
        for (Map.Entry<String, String> app : apps.entrySet()) {
            String exe = app.getKey();
            Switch allowed = new Switch(!s.blockedApps.contains(exe));
            allowed.selectedProperty().addListener((obs, old, now) -> {
                if (now) {
                    s.blockedApps.remove(exe);
                } else {
                    s.blockedApps.add(exe);
                }
                ctx.settingsChanged();
            });
            Label title = UiKit.label(app.getValue(), "muted", "small");
            title.setMaxWidth(420);
            VBox texts = new VBox(2, UiKit.label(prettyName(exe), "strong"), title);
            HBox.setHgrow(texts, Priority.ALWAYS);
            HBox row = new HBox(texts, UiKit.spacer(), allowed);
            row.getStyleClass().add("row");
            if (++index == apps.size()) {
                row.getStyleClass().add("last");
            }
            list.getChildren().add(row);
        }
    }

    /** "chrome.exe" → "Chrome". */
    static String prettyName(String exe) {
        String name = exe.endsWith(".exe") ? exe.substring(0, exe.length() - 4) : exe;
        if (name.isEmpty()) {
            return exe;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public String title() {
        return "Программы";
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        rebuild();
    }
}
