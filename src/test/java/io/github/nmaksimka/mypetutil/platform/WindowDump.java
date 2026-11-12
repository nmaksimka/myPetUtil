package io.github.nmaksimka.mypetutil.platform;

/** Печатает, какие окна видит сканер. Для отладки: gradlew dumpWindows. */
public final class WindowDump {

    private WindowDump() {
    }

    public static void main(String[] args) {
        try (WindowScanner scanner = new WindowScanner()) {
            DesktopSnapshot snapshot = scanner.scan();
            snapshot.monitors().forEach(m -> System.out.println("monitor " + m));
            System.out.println("fullscreen " + snapshot.fullscreenRect() + " fg=" + snapshot.foregroundExe());
            snapshot.windows().forEach(w -> System.out.println(w.exe() + " | " + w.title() + " | " + w.rect()));
        }
    }
}
