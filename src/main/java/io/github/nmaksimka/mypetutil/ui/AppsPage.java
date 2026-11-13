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
import javafx.scene.layout.VBox;

/** Вкладка «Программы»: по окнам каких программ можно гулять. */
final class AppsPage implements Page {

    private final AppContext ctx;
    private final VBox root;
    private final VBox list = new VBox();
    private final Label modeNote;

    AppsPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Программы",
                "Питомцы гуляют по окнам всех программ. Выключите программу, и они перестанут на неё залезать.");

        modeNote = UiKit.text("Окна используются только в режиме «" + BehaviorMode.WINDOWS.title()
                + "». Сейчас выбран другой, его можно сменить на вкладке «Поведение».", "note");

        Switch hide = new Switch(s.hideOnBlockedFullscreen);
        hide.selectedProperty().addListener((obs, old, now) -> {
            s.hideOnBlockedFullscreen = now;
            ctx.settingsChanged();
        });

        list.getStyleClass().add("group");
        HBox listHeader = new HBox(UiKit.heading("Открытые программы"), UiKit.spacer(),
                UiKit.quiet("Обновить", this::rebuild));
        listHeader.setAlignment(Pos.BOTTOM_LEFT);

        root.getChildren().addAll(modeNote, listHeader, list,
                UiKit.heading("Игры на весь экран"),
                UiKit.group(UiKit.row("Прятаться в выключенных программах",
                        "Если выключенная программа открыта на весь экран, питомцы исчезают, пока вы из неё не выйдете. "
                                + "В остальных полноэкранных программах они ходят по самому низу монитора.", hide)));
    }

    private void rebuild() {
        Settings s = ctx.settings();
        boolean otherMode = s.mode != BehaviorMode.WINDOWS;
        modeNote.setVisible(otherMode);
        modeNote.setManaged(otherMode);

        // exe -> пример заголовка; выключенные показываем, даже если сейчас не запущены
        Map<String, String> apps = new TreeMap<>();
        for (RawWindow window : ctx.openWindows()) {
            if (!window.exe().equals("mypetutil.exe") && !window.exe().equals("unknown")) {
                apps.putIfAbsent(window.exe(), window.title());
            }
        }
        for (String blocked : s.blockedApps) {
            apps.putIfAbsent(blocked, "не запущена");
        }

        list.getChildren().clear();
        if (apps.isEmpty()) {
            list.getChildren().add(UiKit.text("Открытых окон нет. Откройте программу и нажмите «Обновить».", "empty"));
            return;
        }
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
            UiKit.addRow(list, UiKit.row(prettyName(exe), app.getValue(), allowed));
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
