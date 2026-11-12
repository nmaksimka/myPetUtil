package io.github.nmaksimka.mypetutil.sprite;

import java.util.Map;

/** Маленькие значки над головой питомца. */
public final class Emotes {

    private static final Map<Character, Integer> PAL = Map.of(
            'r', PixelGrid.rgb(0xF0506E),
            'R', PixelGrid.rgb(0xB8304C),
            'w', PixelGrid.rgb(0xFFFFFF),
            'k', PixelGrid.rgb(0x2B2B33),
            'y', PixelGrid.rgb(0xFFE08A));

    public static final PixelGrid HEART = new PixelGrid(7, 6).stamp(new String[] {
        ".rr.rr.",
        "rwrrrrR",
        "rrrrrrR",
        ".rrrrR.",
        "..rrR..",
        "...R...",
    }, 0, 0, PAL);

    public static final PixelGrid QUESTION = new PixelGrid(5, 7).stamp(new String[] {
        ".kkk.",
        "k...k",
        "....k",
        "..kk.",
        "..k..",
        ".....",
        "..k..",
    }, 0, 0, PAL);

    private Emotes() {
    }
}
