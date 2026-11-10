package io.github.nmaksimka.mypetutil.platform;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/** Функции WinAPI, которых нет в jna-platform. */
final class Win32 {

    static final int GWL_EXSTYLE = -20;
    static final int WS_EX_TOOLWINDOW = 0x00000080;
    static final int WS_EX_APPWINDOW = 0x00040000;
    static final int WS_EX_NOACTIVATE = 0x08000000;
    static final int WS_EX_TOPMOST = 0x00000008;

    static final int GW_OWNER = 4;

    static final int SWP_NOSIZE = 0x0001;
    static final int SWP_NOMOVE = 0x0002;
    static final int SWP_NOACTIVATE = 0x0010;
    static final int SWP_FRAMECHANGED = 0x0020;

    static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    static final int DWMWA_EXTENDED_FRAME_BOUNDS = 9;
    static final int DWMWA_CLOAKED = 14;

    static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;

    private Win32() {
    }

    interface User32Ext extends StdCallLibrary {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean IsIconic(HWND hwnd);

        HWND GetWindow(HWND hwnd, int cmd);
    }

    interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DwmGetWindowAttribute(HWND hwnd, int attribute, RECT value, int size);

        int DwmGetWindowAttribute(HWND hwnd, int attribute, IntByReference value, int size);

        int DwmSetWindowAttribute(HWND hwnd, int attribute, IntByReference value, int size);
    }

    static Pointer topmost() {
        return Pointer.createConstant(-1);
    }
}
