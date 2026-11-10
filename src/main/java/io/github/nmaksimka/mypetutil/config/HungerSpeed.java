package io.github.nmaksimka.mypetutil.config;

/** Как быстро питомец проголодается с полной сытости. */
public enum HungerSpeed {
    SLOW("Медленно", 6 * 60),
    NORMAL("Обычно", 3 * 60),
    FAST("Быстро", 30);

    private final String title;
    private final int minutesToEmpty;

    HungerSpeed(String title, int minutesToEmpty) {
        this.title = title;
        this.minutesToEmpty = minutesToEmpty;
    }

    public String title() {
        return title;
    }

    /** Сколько сытости теряется за секунду. */
    public double perSecond() {
        return PetEntry.MAX_SATIETY / (minutesToEmpty * 60.0);
    }
}
