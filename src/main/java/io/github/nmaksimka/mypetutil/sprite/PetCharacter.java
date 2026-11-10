package io.github.nmaksimka.mypetutil.sprite;

/** Все персонажи, которых можно выпустить на экран. */
public enum PetCharacter {

    CLAUDE("Пупс Клод", "Оранжевый пупсик из Claude Code. Спокойный и любопытный.",
            FoodType.COOKIE, new ClaudeArt(), 1.0),
    SONIC("Соник", "Самый быстрый ёжик. Ходит быстрее всех.",
            FoodType.HOTDOG, new SonicArt(), 1.8),
    GHOST("Призрачок", "Не касается земли и немного стесняется.",
            FoodType.CANDY, new GhostArt(), 0.8),
    MINION("Миньон", "Маленький, жёлтый и вечно голодный.",
            FoodType.BANANA, new MinionArt(), 1.1),
    STEVE("Стив", "Кубический парень. Крафтит, копает, гуляет.",
            FoodType.GOLDEN_APPLE, new SteveArt(), 1.0);

    private final String title;
    private final String description;
    private final FoodType food;
    private final CharacterArt art;
    private final double speedFactor;

    PetCharacter(String title, String description, FoodType food, CharacterArt art, double speedFactor) {
        this.title = title;
        this.description = description;
        this.food = food;
        this.art = art;
        this.speedFactor = speedFactor;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public FoodType food() {
        return food;
    }

    public CharacterArt art() {
        return art;
    }

    /** Множитель скорости ходьбы относительно обычной. */
    public double speedFactor() {
        return speedFactor;
    }

    /** Призрак не стоит на земле, а парит — ему не нужна анимация приземления. */
    public boolean floats() {
        return this == GHOST;
    }
}
