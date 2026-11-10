package io.github.nmaksimka.mypetutil.config;

/** Как питомец ведёт себя на экране. */
public enum BehaviorMode {
    /** Стоит на панели задач и никуда не уходит. */
    STAND("Стоять у панели задач", "Питомец сидит на месте и просто живёт: дышит, моргает, радуется."),
    /** Гуляет по низу экрана, может уходить на другие мониторы. */
    DESKTOP("Гулять по низу экрана", "Ходит вдоль панели задач и переходит между мониторами."),
    /** Гуляет и по нижним краям окон программ. */
    WINDOWS("Гулять по окнам", "Залезает на нижние края окон, уходит в одно окно и выходит из другого.");

    private final String title;
    private final String description;

    BehaviorMode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
