package io.github.nmaksimka.mypetutil.platform;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

/** Какая тема включена в Windows (Параметры → Персонализация → Цвета). */
public final class SystemTheme {

    private static final String KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    private SystemTheme() {
    }

    public static boolean isDark() {
        try {
            return Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, KEY, "AppsUseLightTheme") == 0;
        } catch (Throwable e) {
            return false;
        }
    }
}
