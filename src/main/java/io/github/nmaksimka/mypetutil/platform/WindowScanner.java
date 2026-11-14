package io.github.nmaksimka.mypetutil.platform;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.Monitor;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.PhysRect;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot.RawWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Раз в 400 мс в фоне спрашивает у Windows список окон и мониторов.
 * Последний результат лежит в {@link #latest()}, JavaFX-поток его только читает.
 */
public final class WindowScanner implements AutoCloseable {

    /** Заголовки наших собственных окошек: по ним не гуляем. */
    public static final String OWN_WINDOW_PREFIX = "myPetUtil·";

    private static final Logger LOG = Logger.getLogger(WindowScanner.class.getName());

    private static final Set<String> SHELL_CLASSES = Set.of(
            "Progman", "WorkerW", "Shell_TrayWnd", "Shell_SecondaryTrayWnd",
            "Windows.UI.Core.CoreWindow", "ApplicationFrameWindow_Hidden", "XamlExplorerHostIslandWindow");

    private final AtomicReference<DesktopSnapshot> latest = new AtomicReference<>(DesktopSnapshot.EMPTY);
    private final Map<Integer, String> exeByPid = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "window-scanner");
        thread.setDaemon(true);
        return thread;
    });
    private final int ownPid = Kernel32.INSTANCE.GetCurrentProcessId();
    private int scans;

    public void start() {
        executor.scheduleWithFixedDelay(this::scanSafely, 0, 400, TimeUnit.MILLISECONDS);
    }

    public DesktopSnapshot latest() {
        return latest.get();
    }

    private void scanSafely() {
        try {
            DesktopSnapshot next = scan();
            // если на экране ничего не поменялось, оставляем старый объект:
            // по нему менеджер понимает, что пересобирать «мир» не нужно
            if (!next.equals(latest.get())) {
                latest.set(next);
            }
            if (++scans % 150 == 0) {
                exeByPid.clear(); // pid-ы переиспользуются, раз в минуту чистим кеш
            }
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "window scan failed", e);
        }
    }

    DesktopSnapshot scan() {
        List<Monitor> monitors = monitors();
        List<RawWindow> windows = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            RawWindow window = describe(hwnd);
            if (window != null) {
                windows.add(window);
            }
            return true;
        }, null);

        PhysRect fullscreen = null;
        String foregroundExe = null;
        HWND foreground = User32.INSTANCE.GetForegroundWindow();
        if (foreground != null && !SHELL_CLASSES.contains(className(foreground))) {
            foregroundExe = exeOf(foreground);
            PhysRect rect = windowRect(foreground);
            for (Monitor monitor : monitors) {
                if (rect != null && rect.covers(monitor.bounds())) {
                    fullscreen = monitor.bounds();
                    break;
                }
            }
        }
        return new DesktopSnapshot(monitors, windows, fullscreen, foregroundExe);
    }

    private RawWindow describe(HWND hwnd) {
        if (!User32.INSTANCE.IsWindowVisible(hwnd) || Win32.User32Ext.INSTANCE.IsIconic(hwnd)) {
            return null;
        }
        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
        if ((exStyle & Win32.WS_EX_TOOLWINDOW) != 0) {
            return null;
        }
        if (isCloaked(hwnd)) {
            return null;
        }
        String title = title(hwnd);
        if (title.isBlank() || title.startsWith(OWN_WINDOW_PREFIX)) {
            return null;
        }
        if (SHELL_CLASSES.contains(className(hwnd))) {
            return null;
        }
        PhysRect rect = windowRect(hwnd);
        if (rect == null || rect.width() < 120 || rect.height() < 80) {
            return null;
        }
        return new RawWindow(Pointer.nativeValue(hwnd.getPointer()), rect, exeOf(hwnd), title);
    }

    private static boolean isCloaked(HWND hwnd) {
        try {
            IntByReference cloaked = new IntByReference();
            int result = Win32.Dwmapi.INSTANCE.DwmGetWindowAttribute(hwnd, Win32.DWMWA_CLOAKED, cloaked, 4);
            return result == 0 && cloaked.getValue() != 0;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Видимые границы окна без невидимой «тени» вокруг, которую добавляет Windows 10/11. */
    private static PhysRect windowRect(HWND hwnd) {
        RECT rect = new RECT();
        try {
            if (Win32.Dwmapi.INSTANCE.DwmGetWindowAttribute(hwnd, Win32.DWMWA_EXTENDED_FRAME_BOUNDS, rect, rect.size()) == 0) {
                return new PhysRect(rect.left, rect.top, rect.right, rect.bottom);
            }
        } catch (Throwable ignored) {
            // нет dwm — берём обычный прямоугольник
        }
        if (User32.INSTANCE.GetWindowRect(hwnd, rect)) {
            return new PhysRect(rect.left, rect.top, rect.right, rect.bottom);
        }
        return null;
    }

    private static String title(HWND hwnd) {
        char[] buffer = new char[256];
        int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return new String(buffer, 0, Math.max(0, length));
    }

    private static String className(HWND hwnd) {
        char[] buffer = new char[128];
        int length = User32.INSTANCE.GetClassName(hwnd, buffer, buffer.length);
        return new String(buffer, 0, Math.max(0, length));
    }

    private String exeOf(HWND hwnd) {
        IntByReference pidRef = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
        int pid = pidRef.getValue();
        if (pid == ownPid) {
            return "mypetutil.exe";
        }
        return exeByPid.computeIfAbsent(pid, WindowScanner::queryExe);
    }

    private static String queryExe(int pid) {
        HANDLE process = Kernel32.INSTANCE.OpenProcess(Win32.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (process == null) {
            return "unknown";
        }
        try {
            char[] buffer = new char[1024];
            IntByReference size = new IntByReference(buffer.length);
            if (!Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, size)) {
                return "unknown";
            }
            String path = new String(buffer, 0, size.getValue());
            int slash = path.lastIndexOf('\\');
            return path.substring(slash + 1).toLowerCase();
        } finally {
            Kernel32.INSTANCE.CloseHandle(process);
        }
    }

    private static List<Monitor> monitors() {
        List<Monitor> monitors = new ArrayList<>();
        User32.INSTANCE.EnumDisplayMonitors(null, null, (hMonitor, hdc, rect, data) -> {
            WinUser.MONITORINFO info = new WinUser.MONITORINFO();
            if (User32.INSTANCE.GetMonitorInfo(hMonitor, info).booleanValue()) {
                monitors.add(new Monitor(
                        new PhysRect(info.rcMonitor.left, info.rcMonitor.top, info.rcMonitor.right, info.rcMonitor.bottom),
                        new PhysRect(info.rcWork.left, info.rcWork.top, info.rcWork.right, info.rcWork.bottom),
                        (info.dwFlags & WinUser.MONITORINFOF_PRIMARY) != 0));
            }
            return 1;
        }, null);
        return monitors;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
