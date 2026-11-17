package io.github.nmaksimka.mypetutil.platform;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Настройка наших окон через WinAPI. Окно ищется по уникальному заголовку. */
public final class NativeWindows {

    private static final Logger LOG = Logger.getLogger(NativeWindows.class.getName());

    private NativeWindows() {
    }

    public static HWND find(String title) {
        try {
            return User32.INSTANCE.FindWindow(null, title);
        } catch (Throwable e) {
            LOG.log(Level.FINE, "FindWindow failed", e);
            return null;
        }
    }

    /**
     * Делает окно «невидимым» для системы: нет кнопки на панели задач, нет в Alt+Tab,
     * клик по питомцу не забирает фокус у браузера или игры.
     */
    public static void makeOverlay(HWND hwnd) {
        if (hwnd == null) {
            return;
        }
        try {
            int style = User32.INSTANCE.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
            style |= Win32.WS_EX_TOOLWINDOW | Win32.WS_EX_NOACTIVATE;
            style &= ~Win32.WS_EX_APPWINDOW;
            User32.INSTANCE.SetWindowLongPtr(hwnd, Win32.GWL_EXSTYLE, new Pointer(style & 0xFFFFFFFFL));
            bringToTop(hwnd);
        } catch (Throwable e) {
            LOG.log(Level.FINE, "cannot change window style", e);
        }
    }

    /** Поднимает окно поверх всех, в том числе поверх полноэкранных окон без рамки. */
    public static void bringToTop(HWND hwnd) {
        if (hwnd == null) {
            return;
        }
        try {
            User32.INSTANCE.SetWindowPos(hwnd, new HWND(Win32.topmost()), 0, 0, 0, 0,
                    Win32.SWP_NOMOVE | Win32.SWP_NOSIZE | Win32.SWP_NOACTIVATE);
        } catch (Throwable e) {
            LOG.log(Level.FINE, "SetWindowPos failed", e);
        }
    }

    /**
     * Сдвигает окно средствами Windows, в физических пикселях. JavaFX при этом не перерисовывает
     * содержимое окна (при stage.setX — перерисовывает и заново копирует картинку из видеокарты).
     */
    public static boolean move(HWND hwnd, int x, int y) {
        if (hwnd == null) {
            return false;
        }
        try {
            return User32.INSTANCE.SetWindowPos(hwnd, null, x, y, 0, 0,
                    Win32.SWP_NOSIZE | Win32.SWP_NOZORDER | Win32.SWP_NOACTIVATE);
        } catch (Throwable e) {
            LOG.log(Level.FINE, "SetWindowPos move failed", e);
            return false;
        }
    }

    /** Тёмный заголовок окна в Windows 10/11. */
    public static void setDarkTitleBar(HWND hwnd, boolean dark) {
        if (hwnd == null) {
            return;
        }
        try {
            Win32.Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, Win32.DWMWA_USE_IMMERSIVE_DARK_MODE,
                    new IntByReference(dark ? 1 : 0), 4);
            // заставляем Windows перерисовать рамку
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    Win32.SWP_NOMOVE | Win32.SWP_NOSIZE | Win32.SWP_NOACTIVATE | Win32.SWP_FRAMECHANGED | Win32.SWP_NOZORDER);
        } catch (Throwable e) {
            LOG.log(Level.FINE, "cannot set dark title bar", e);
        }
    }
}
