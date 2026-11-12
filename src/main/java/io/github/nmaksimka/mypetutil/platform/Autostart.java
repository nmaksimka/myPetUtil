package io.github.nmaksimka.mypetutil.platform;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Запуск вместе с Windows через ключ реестра HKCU\...\Run. Права администратора не нужны. */
public final class Autostart {

    private static final Logger LOG = Logger.getLogger(Autostart.class.getName());
    private static final String RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE = "myPetUtil";

    private Autostart() {
    }

    /**
     * Путь к exe. Есть только у собранной версии (jpackage кладёт его в свойство jpackage.app-path).
     * Из IDEA автозапуск включить нельзя — там нет exe.
     */
    public static String exePath() {
        return System.getProperty("jpackage.app-path");
    }

    public static boolean available() {
        return exePath() != null;
    }

    public static boolean isEnabled() {
        try {
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE);
        } catch (Throwable e) {
            return false;
        }
    }

    /** @return получилось ли */
    public static boolean setEnabled(boolean enabled) {
        try {
            if (enabled) {
                if (!available()) {
                    return false;
                }
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE,
                        "\"" + exePath() + "\" --tray");
            } else if (isEnabled()) {
                Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE);
            }
            return true;
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "cannot change autostart", e);
            return false;
        }
    }
}
