package io.github.nmaksimka.mypetutil.sprite;

import java.util.Map;

/** Еда. У каждого питомца своя любимая, чужую он не ест. */
public enum FoodType {

    COOKIE("Печенька", new String[] {
        "..........",
        "...CCCC...",
        "..CCkCCC..",
        ".CCCCCkCC.",
        ".CkCCCCCC.",
        ".CCCCkCCc.",
        ".cCkCCCCc.",
        "..cCCCCc..",
        "...cccc...",
        "..........",
    }),

    HOTDOG("Хот-дог", new String[] {
        "..........",
        "..........",
        "...BBBB...",
        ".bBBBBBBb.",
        "SSYSSYSSYS",
        "sSSYSSYSSs",
        ".bBBBBBBb.",
        "..bbbbbb..",
        "..........",
        "..........",
    }),

    CANDY("Конфета", new String[] {
        "..........",
        "..........",
        "E...PP...E",
        "EE.PPwP.EE",
        "EEEPwPPEEE",
        "EE.PPPp.EE",
        "E...pp...E",
        "..........",
        "..........",
        "..........",
    }),

    BANANA("Банан", new String[] {
        "........k.",
        ".......YY.",
        "......YYy.",
        "......YYy.",
        ".....YYYy.",
        "k...YYYy..",
        "YYYYYYyy..",
        ".yyYYyy...",
        "..yyyy....",
        "..........",
    }),

    GOLDEN_APPLE("Золотое яблоко", new String[] {
        "....t.....",
        "....tLL...",
        "..GGtGGG..",
        ".GhGGGGGG.",
        ".GhGGGGGg.",
        ".GGGGGGGg.",
        ".GGGGGGgg.",
        "..gGGGGg..",
        "...gg.g...",
        "..........",
    });

    public static final int SIZE = 10;

    private final String title;
    private final PixelGrid grid;

    FoodType(String title, String[] rows) {
        this.title = title;
        this.grid = new PixelGrid(SIZE, SIZE).stamp(rows, 0, 0, Colors.PALETTE);
    }

    public String title() {
        return title;
    }

    /** Общий объект — не изменять. */
    public PixelGrid grid() {
        return grid;
    }
    /** Палитра в отдельном классе: из конструктора enum нельзя читать его же static-поля. */
    private static final class Colors {
        static final Map<Character, Integer> PALETTE = Map.ofEntries(
                Map.entry('C', PixelGrid.rgb(0xD79B5B)),
                Map.entry('c', PixelGrid.rgb(0xAF733A)),
                Map.entry('k', PixelGrid.rgb(0x4A2C17)),
                Map.entry('B', PixelGrid.rgb(0xE6AE5E)),
                Map.entry('b', PixelGrid.rgb(0xC48838)),
                Map.entry('S', PixelGrid.rgb(0xC9483B)),
                Map.entry('s', PixelGrid.rgb(0x8F2C25)),
                Map.entry('Y', PixelGrid.rgb(0xF5D547)),
                Map.entry('y', PixelGrid.rgb(0xD6AB27)),
                Map.entry('P', PixelGrid.rgb(0xF07AA8)),
                Map.entry('p', PixelGrid.rgb(0xC6507F)),
                Map.entry('w', PixelGrid.rgb(0xFFFFFF)),
                Map.entry('E', PixelGrid.rgb(0xF8B9D2)),
                Map.entry('G', PixelGrid.rgb(0xF2C232)),
                Map.entry('g', PixelGrid.rgb(0xC7951B)),
                Map.entry('h', PixelGrid.rgb(0xFFF3B0)),
                Map.entry('L', PixelGrid.rgb(0x5DAA3C)),
                Map.entry('t', PixelGrid.rgb(0x6B4A2A)));
    }
}
