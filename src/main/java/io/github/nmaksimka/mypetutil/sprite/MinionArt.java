package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

import java.util.Map;

/** Жёлтый человечек в комбинезоне и с одним большим очком. */
final class MinionArt extends CharacterArt {

    private static final int YELLOW = rgb(0xF6D33C);
    private static final int YELLOW_DARK = rgb(0xD6AE22);
    private static final int RIM = rgb(0x9AA1AA);
    private static final int WHITE = rgb(0xFFFFFF);
    private static final int PUPIL = rgb(0x5B3A1E);
    private static final int BLACK = rgb(0x26262B);
    private static final int MOUTH = rgb(0x6E3B1F);

    private static final Map<Character, Integer> PAL = palette(
            'Y', 0xF6D33C, 'y', 0xD6AE22, 'K', 0x26262B, 'U', 0x3A66B8, 'u', 0x2A4C8C);

    private static final String[] HEAD = {
        "......YYYY......",
        ".....YYYYYY.....",
        "....YYYYYYYY....",
        "...yYYYYYYYYY...",
        "..KKYYYYYYYYKK..",
        "..KKYYYYYYYYKK..",
        "...yYYYYYYYYY...",
        "...yYYYYYYYYY...",
        "...yYYYYYYYYY...",
    };

    private static final String[] OVERALLS = {
        "...yUYYYYYYUY...",
        "...UUUUUUUUUU...",
        "...uUUUuuUUUU...",
        "....uUUUUUUU....",
    };

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int f = pose.frame();
        Anim anim = pose.anim();
        int lift = switch (anim) {
            case HOVER -> f == 0 ? -2 : 0;
            case WALK -> (f == 1 || f == 3) ? -1 : 0;
            case SAD -> 2;
            case LAND -> 1;
            default -> 0;
        };
        int top = lift;
        g.stamp(HEAD, 0, top, PAL);
        g.stamp(OVERALLS, 0, top + HEAD.length, PAL);

        drawGoggle(g, face, top + 3, anim == Anim.SAD && f == 1);
        drawMouth(g, face, f, top + 8);

        int armY = top + 8;
        switch (anim) {
            case HOVER, DRAG, FALL -> {
                g.vline(2, armY - 3, 3, YELLOW).px(2, armY - 4, BLACK);
                g.vline(13, armY - 3, 3, YELLOW).px(13, armY - 4, BLACK);
            }
            case WALK -> {
                int swing = (f == 1) ? -1 : (f == 3 ? 1 : 0);
                g.vline(2, armY + swing, 2, YELLOW).px(2, armY + swing + 2, BLACK);
                g.vline(13, armY - swing, 2, YELLOW).px(13, armY - swing + 2, BLACK);
            }
            case EAT -> {
                g.vline(2, armY, 2, YELLOW).px(2, armY + 2, BLACK);
                g.px(11, armY + 1, YELLOW).px(12, armY, BLACK);
            }
            default -> {
                int droop = anim == Anim.SAD ? 1 : 0;
                g.vline(2, armY + droop, 2, YELLOW).px(2, armY + 2 + droop, BLACK);
                g.vline(13, armY + droop, 2, YELLOW).px(13, armY + 2 + droop, BLACK);
            }
        }

        int legTop = top + HEAD.length + OVERALLS.length;
        switch (anim) {
            case SAD -> {
                g.hline(4, 15, 3, BLACK);
                g.hline(9, 15, 3, BLACK);
            }
            case DRAG, FALL -> {
                int swing = f == 0 ? 0 : 1;
                legs(g, legTop, 5, 10, 15 - swing, 15);
            }
            case WALK -> {
                int[][] feet = {{5, 10}, {4, 11}, {5, 10}, {6, 9}};
                legs(g, legTop, feet[f][0], feet[f][1], f == 1 ? 14 : 15, f == 3 ? 14 : 15);
            }
            default -> legs(g, legTop, 5, 10, 15, 15);
        }
    }

    private static void legs(PixelGrid g, int legTop, int leftX, int rightX, int leftFoot, int rightFoot) {
        int blue = rgb(0x2A4C8C);
        for (int y = legTop; y < leftFoot; y++) {
            g.px(6, y, blue);
        }
        for (int y = legTop; y < rightFoot; y++) {
            g.px(9, y, blue);
        }
        g.hline(leftX - 1, leftFoot, 3, BLACK);
        g.hline(rightX - 1, rightFoot, 3, BLACK);
    }

    /** Большое очко 6x5, (y) — верх оправы. */
    private static void drawGoggle(PixelGrid g, Face face, int y, boolean tear) {
        g.rect(5, y, 6, 5, RIM);
        g.rect(6, y + 1, 4, 3, WHITE);
        switch (face) {
            case NORMAL -> g.rect(8, y + 1, 2, 2, PUPIL);
            case SURPRISED -> g.px(8, y + 2, PUPIL);
            case BLINK -> {
                g.rect(6, y + 1, 4, 3, YELLOW);
                g.hline(6, y + 3, 4, YELLOW_DARK);
            }
            case HAPPY, EATING -> {
                g.rect(6, y + 1, 4, 3, YELLOW);
                g.px(6, y + 3, PUPIL);
                g.hline(7, y + 2, 2, PUPIL);
                g.px(9, y + 3, PUPIL);
            }
            case SAD -> {
                g.hline(6, y + 1, 4, YELLOW);
                g.rect(7, y + 2, 2, 2, PUPIL);
                if (tear) {
                    g.px(10, y + 5, TEAR);
                    g.px(10, y + 6, TEAR);
                }
            }
        }
    }

    private static void drawMouth(PixelGrid g, Face face, int frame, int y) {
        switch (face) {
            case EATING -> {
                if (frame == 0) {
                    g.rect(7, y, 3, 2, MOUTH);
                } else {
                    g.hline(7, y, 3, MOUTH);
                }
            }
            case HAPPY -> {
                g.px(6, y - 1, MOUTH);
                g.hline(7, y, 3, MOUTH);
                g.px(10, y - 1, MOUTH);
            }
            case SAD -> {
                g.hline(7, y - 1, 3, MOUTH);
                g.px(6, y, MOUTH);
                g.px(10, y, MOUTH);
            }
            case SURPRISED -> g.rect(8, y - 1, 2, 2, MOUTH);
            default -> g.hline(7, y, 3, MOUTH);
        }
    }
}
