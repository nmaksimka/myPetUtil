package io.github.nmaksimka.mypetutil.config;

/**
 * Темы окна настроек. Каждая — набор из нескольких цветов, которые подставляются
 * в CSS как переменные (-bg, -text, -accent и т.д.).
 */
public enum Theme {

    SYSTEM("Как в Windows", null),
    SNOW("Снег", new Palette(
            "#F6F7F9", "#FFFFFF", "#E2E5EA", "#EDEFF3", "#16181D", "#687080", "#2F6BE0", "#FFFFFF", false)),
    GRAPHITE("Графит", new Palette(
            "#1D1F23", "#25282D", "#353940", "#2D3036", "#E7E9ED", "#959BA6", "#86A8F5", "#14171C", true)),
    SAKURA("Сакура", new Palette(
            "#FBF4F6", "#FFFFFF", "#EEDBE2", "#F6E8ED", "#2B1F24", "#8B7179", "#CF5D86", "#FFFFFF", false)),
    MINT("Мята", new Palette(
            "#F1F7F4", "#FFFFFF", "#D5E6DE", "#E5F1EB", "#13221C", "#62786E", "#23926F", "#FFFFFF", false)),
    NIGHT("Ночь", new Palette(
            "#101728", "#172036", "#26324D", "#1D2841", "#DDE4F3", "#8693AE", "#F0B54A", "#1A1405", true)),
    GAMEBOY("Геймбой", new Palette(
            "#C9D99B", "#D8E5AE", "#8FAA3E", "#BCCF86", "#0F380F", "#3B6B2A", "#306230", "#E0F0B8", false));

    /**
     * @param bg       фон окна
     * @param surface  фон списков и полей
     * @param line     линии и рамки
     * @param hover    подсветка при наведении
     * @param text     основной текст
     * @param muted    второстепенный текст
     * @param accent   акцент: выбранное, главные кнопки
     * @param onAccent текст на акцентном фоне
     * @param dark     тёмная ли тема (для заголовка окна Windows)
     */
    public record Palette(String bg, String surface, String line, String hover, String text, String muted,
                          String accent, String onAccent, boolean dark) {

        /** Строка для setStyle: объявляет цвета для CSS. */
        public String toCss() {
            return "-bg: " + bg + "; -surface: " + surface + "; -line: " + line + "; -hover: " + hover
                    + "; -text: " + text + "; -muted: " + muted + "; -accent: " + accent
                    + "; -on-accent: " + onAccent + ";";
        }
    }

    private final String title;
    private final Palette palette;

    Theme(String title, Palette palette) {
        this.title = title;
        this.palette = palette;
    }

    public String title() {
        return title;
    }

    /** Цвета темы. Для SYSTEM выбирается Снег или Графит по теме Windows. */
    public Palette palette(boolean systemDark) {
        if (this == SYSTEM) {
            return systemDark ? GRAPHITE.palette : SNOW.palette;
        }
        return palette;
    }
}
