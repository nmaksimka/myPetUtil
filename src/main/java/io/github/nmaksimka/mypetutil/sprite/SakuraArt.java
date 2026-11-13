package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

import java.util.Map;

/** Деревце сакуры в горшочке. Глазки нарисованы на горшке, ходит вприпрыжку. */
final class SakuraArt extends CharacterArt {

    private static final int POT = rgb(0xC9774B);
    private static final int POT_DARK = rgb(0xA35A35);
    private static final int POT_RIM = rgb(0xDA8C60);
    private static final int EYE = rgb(0x2B1D18);
    private static final int BLUSH = rgb(0xF29BB4);
    private static final int MOUTH = rgb(0x5E2A1C);
    private static final int PETAL = rgb(0xF6B3C9);

    private static final Map<Character, Integer> BLOOM = palette(
            'P', 0xF6B3C9, 'p', 0xE6829F, 'L', 0xFCDCE7, 'w', 0xFFF6F9, 'T', 0x6E4A3A);

    /** Та же крона, но вялая — когда деревце голодное. */
    private static final Map<Character, Integer> WILTED = palette(
            'P', 0xD3A9B6, 'p', 0xB98A99, 'L', 0xE2CCD3, 'w', 0xEEE2E6, 'T', 0x6E4A3A);

    private static final String[] CROWN = {
        "....LPPPL.......",
        "..LPPwPPPPL.....",
        ".PPPPPPPwPPP....",
        "LPwPPPPPPPPPPL..",
        "PPPPPpPPPPwPPP..",
        ".PPpPPPPPPPPpP..",
        "..PPPPTPPpPP....",
        "....pp.T.pp.....",
    };

    private static final String[] DROOPY = {
        "................",
        "...LPPPPPL......",
        ".LPPwPPPPPPL....",
        "PPPPPPPPwPPPP...",
        "PPpPPPPPPPPpP...",
        ".pPPpPPTPPpPp...",
        "..p.ppPT.pp.p...",
        "....p..T..p.....",
    };

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int f = pose.frame();
        Anim anim = pose.anim();
        int lift = switch (anim) {
            case WALK -> (f == 1 || f == 3) ? -2 : 0;
            case HOVER -> f == 0 ? -2 : 0;
            case LAND -> 1;
            default -> 0;
        };
        // крона чуть отстаёт от горшка и качается
        int sway = switch (anim) {
            case IDLE -> f == 0 ? 0 : 1;
            case WALK -> f == 1 ? -1 : (f == 3 ? 1 : 0);
            case DRAG, FALL -> f == 0 ? -1 : 1;
            default -> 0;
        };
        boolean sad = anim == Anim.SAD;

        int crownTop = 1 + lift + (anim == Anim.LAND ? 1 : 0);
        g.stamp(sad ? DROOPY : CROWN, 1 + sway, crownTop, sad ? WILTED : BLOOM);
        // ствол
        g.vline(8, crownTop + 7, 3 - (anim == Anim.LAND ? 1 : 0), rgb(0x6E4A3A));

        int potTop = 10 + lift;
        if (anim == Anim.LAND) {
            potTop = 11;
        }
        g.hline(3, potTop, 10, POT_RIM);
        g.hline(3, potTop + 1, 10, POT_DARK);
        int bottom = 15 + lift;
        for (int y = potTop + 2; y <= bottom; y++) {
            int inset = (y - potTop - 2) / 2;
            g.hline(4 + inset, y, 8 - inset * 2, POT);
        }
        g.hline(5, bottom, 6, POT_DARK);

        int eyeY = potTop + 3;
        drawEyes(g, face, eyeY, sad && f == 1);
        g.px(5, eyeY + 1, BLUSH);
        g.px(10, eyeY + 1, BLUSH);
        drawMouth(g, face, f, eyeY + 1);

        if (sad) {
            // опавшие лепестки у горшка
            g.px(1, 15, PETAL);
            g.px(13, 14 + f, PETAL);
        }
        if (anim == Anim.HOVER || anim == Anim.EAT && f == 0) {
            g.px(0, crownTop + 1, PETAL);
            g.px(15, crownTop + 3, PETAL);
        }
    }

    private static void drawEyes(PixelGrid g, Face face, int y, boolean tear) {
        int[] xs = {6, 9};
        for (int x : xs) {
            switch (face) {
                case NORMAL -> g.vline(x, y - 1, 2, EYE);
                case BLINK -> g.px(x, y, EYE);
                case HAPPY, EATING -> {
                    g.px(x - 1, y, EYE);
                    g.px(x, y - 1, EYE);
                    g.px(x + 1, y, EYE);
                }
                case SURPRISED -> g.vline(x, y - 2, 3, EYE);
                case SAD -> {
                    g.px(x, y, EYE);
                    g.px(x == 6 ? x - 1 : x + 1, y - 1, EYE);
                    if (tear) {
                        g.px(x, y + 1, TEAR);
                    }
                }
            }
        }
    }

    private static void drawMouth(PixelGrid g, Face face, int frame, int y) {
        switch (face) {
            case EATING -> {
                if (frame == 0) {
                    g.rect(7, y, 2, 2, MOUTH);
                } else {
                    g.hline(7, y + 1, 2, MOUTH);
                }
            }
            case SURPRISED -> g.rect(7, y, 2, 2, MOUTH);
            case HAPPY -> {
                g.px(7, y + 1, MOUTH);
                g.px(8, y + 1, MOUTH);
            }
            case SAD -> {
                g.px(7, y + 1, MOUTH);
                g.px(8, y + 1, MOUTH);
                g.px(6, y + 2, MOUTH);
                g.px(9, y + 2, MOUTH);
            }
            default -> g.px(7, y + 1, MOUTH);
        }
    }
}
