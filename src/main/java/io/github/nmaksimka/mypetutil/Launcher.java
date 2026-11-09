package io.github.nmaksimka.mypetutil;

/**
 * Точка входа. Отдельный класс без наследования от Application нужен,
 * чтобы JavaFX запускался из обычного jar / exe без module-path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        PetApp.main(args);
    }
}
