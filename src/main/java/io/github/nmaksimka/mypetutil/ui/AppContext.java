package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.overlay.PetManager;

import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;

import java.nio.file.Path;
import java.util.List;

/** То, что окну настроек нужно от приложения. */
public interface AppContext {

    Settings settings();

    PetManager pets();

    /** Настройки поменялись — сохранить. */
    void settingsChanged();

    /** Поменялась тема — перекрасить окна. */
    void themeChanged();

    Path settingsFile();

    /** Окна, открытые прямо сейчас (для вкладки «Программы»). */
    default List<RawWindow> openWindows() {
        return pets().currentWindows();
    }

    void exit();
}
