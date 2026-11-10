package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

import java.util.Map;

/** Маленький призрак: парит над землёй, хвостик колышется. */
final class GhostArt extends CharacterArt {

    private static final int WHITE = rgb(0xF4F3FB);
    private static final int SHADE = rgb(0xC8C6DE);
    private static final int EYE = rgb(0x2A2840);
    private static final int BLUSH = rgb(0xF5A3C0);
    private static final int MOUTH = rgb(0x4A3F66);

    private static final Map<Character, Integer> PAL = palette('W', 0xF4F3FB, 'S', 0xC8C6DE);

    private static final String[] BODY = {
        ".....WWWWW......",
        "...WWWWWWWWW....",
        "..WWWWWWWWWWW...",
        "..WWWWWWWWWWW...",
        ".SWWWWWWWWWWWW..",
        ".SWWWWWWWWWWWW..",
        ".SWWWWWWWWWWWW..",
        ".SWWWWWWWWWWWW..",
        ".SWWWWWWWWWWWW..",
        ".SSWWWWWWWWWWW..",
    };

    private static final String[][] TAILS = {
        {
            ".SSWWWWWWWWWWS..",
            ".SS.WWW..WWW.S..",
            ".S...W....W.....",
        },
        {
            ".SSWWWWWWWWWWS..",
            "..SWW..WWW..WW..",
            "...W....W....W..",
        },
    };

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int f = pose.frame();
        Anim anim = pose.anim();
        int lift = switch (anim) {
            case IDLE -> f == 0 ? 0 : -1;
            case WALK -> (f == 1 || f == 2) ? -1 : 0;
            case HOVER -> f == 0 ? -3 : -1;
            case SAD, LAND -> 1;
            default -> 0;
        };
        int top = 1 + lift;
        g.stamp(BODY, 0, top, PAL);
        int tail = switch (anim) {
            case WALK -> f % 2;
            case IDLE, HOVER, DRAG, EAT -> f;
            default -> 0;
        };
        g.stamp(TAILS[tail], 0, top + BODY.length, PAL);

        // ручки-отростки
        if (anim == Anim.DRAG || anim == Anim.FALL || anim == Anim.HOVER) {
            g.px(1, top + 3, WHITE).px(0, top + 2, WHITE);
            g.px(14, top + 3, WHITE).px(15, top + 2, WHITE);
        } else if (anim == Anim.SAD) {
            g.px(0, top + 8, SHADE);
            g.px(14, top + 8, WHITE);
        } else {
            g.px(0, top + 6, SHADE);
            g.px(14, top + 6, WHITE).px(15, top + 7, WHITE);
        }

        int eyeY = top + 4;
        boolean tear = anim == Anim.SAD && f == 1;
        tallEye(g, face, 8, eyeY, EYE, tear);
        tallEye(g, face, 11, eyeY, EYE, tear);
        g.px(7, eyeY + 3, BLUSH);
        g.px(12, eyeY + 3, BLUSH);

        int mouthY = eyeY + 3;
        switch (face) {
            case EATING -> {
                if (f == 0) {
                    g.rect(9, mouthY, 2, 2, MOUTH);
                } else {
                    g.hline(9, mouthY + 1, 2, MOUTH);
                }
            }
            case SURPRISED -> g.rect(9, mouthY, 2, 2, MOUTH);
            case SAD -> g.hline(9, mouthY + 1, 2, MOUTH);
            case HAPPY -> g.hline(9, mouthY, 2, MOUTH);
            default -> g.px(10, mouthY, MOUTH);
        }
    }
}
