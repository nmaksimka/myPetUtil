package io.github.nmaksimka.mypetutil.config;

public enum ThemeMode {
    SYSTEM("Как в Windows"),
    LIGHT("Светлая"),
    DARK("Тёмная");

    private final String title;

    ThemeMode(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
