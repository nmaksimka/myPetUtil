package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

import java.util.Map;

/** Синий ёжик в профиль: иголки назад, большой глаз, красные кроссовки. */
final class SonicArt extends CharacterArt {

    private static final int BLUE = rgb(0x2457D6);
    private static final int DARK = rgb(0x173A97);
    private static final int SKIN = rgb(0xF2C08A);
    private static final int WHITE = rgb(0xFFFFFF);
    private static final int BLACK = rgb(0x111111);
    private static final int RED = rgb(0xD9262B);
    private static final int RED_DARK = rgb(0x951519);
    private static final int GLOVE = rgb(0xF4F4F4);
    private static final int MOUTH = rgb(0x7A2A1E);

    private static final Map<Character, Integer> PAL = palette(
            'B', 0x2457D6, 'b', 0x173A97, 's', 0xF2C08A, 'w', 0xFFFFFF, 'k', 0x111111);

    private static final String[] HEAD = {
        ".......bB.......",
        "...bBBBBBBB.....",
        "bBBBBBBBBBBB....",
        ".bBBBBBBwwwB....",
        "...BBBBBwwwws...",
        "bBBBBBBBwwwss...",
        ".bBBBBBBBsssssk.",
        "...BBBBBssssss..",
        "..bBBBBBBssss...",
        ".....BBBBBs.....",
    };

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int f = pose.frame();
        Anim anim = pose.anim();
        int lift = 0;
        if (anim == Anim.HOVER && f == 0) {
            lift = -2;
        }
        if (anim == Anim.WALK && (f == 1 || f == 3)) {
            lift = -1;
        }
        if (anim == Anim.SAD) {
            lift = 2;
        }

        g.stamp(HEAD, 0, lift, PAL);
        drawEye(g, face, lift, anim == Anim.SAD && f == 1);
        drawMouth(g, face, f, lift);

        int bodyY = 10 + lift;
        g.rect(6, bodyY, 3, 3, BLUE);
        g.vline(8, bodyY, 3, SKIN);
        g.px(6, bodyY + 2, DARK);

        switch (anim) {
            case DRAG, FALL -> {
                // руки вверх, ноги болтаются
                g.vline(9, bodyY - 1, 1, BLUE);
                g.rect(10, bodyY - 3, 2, 2, GLOVE);
                g.rect(3, bodyY - 2, 2, 2, GLOVE);
                g.px(5, bodyY, BLUE);
                int swing = f == 0 ? 0 : 1;
                leg(g, 6, bodyY + 3, 15, 4 - swing);
                leg(g, 8, bodyY + 3, 14 + swing, 9 + swing);
            }
            case SAD -> {
                // сидит, обняв колени
                g.rect(7, 13, 4, 1, BLUE);
                g.rect(9, bodyY + 1, 2, 2, GLOVE);
                shoe(g, 10, 15);
                shoe(g, 5, 15);
            }
            case LAND -> {
                g.rect(9, bodyY + 1, 2, 2, GLOVE);
                leg(g, 6, bodyY + 3, 15, 3);
                leg(g, 8, bodyY + 3, 15, 9);
            }
            case WALK -> {
                int[][] steps = {{5, 8}, {3, 10}, {6, 7}, {9, 4}};
                int[] st = steps[f];
                int handX = f == 1 ? 10 : (f == 3 ? 3 : 9);
                g.rect(handX, bodyY + 1, 2, 2, GLOVE);
                leg(g, 6, bodyY + 3, f == 1 ? 14 : 15, st[0]);
                leg(g, 8, bodyY + 3, f == 3 ? 14 : 15, st[1]);
            }
            default -> {
                if (anim == Anim.EAT) {
                    g.rect(11, bodyY - 1, 2, 2, GLOVE);
                } else {
                    int handY = anim == Anim.IDLE && f == 1 ? bodyY + 2 : bodyY + 1;
                    g.rect(9, handY, 2, 2, GLOVE);
                }
                leg(g, 6, bodyY + 3, 15, 4);
                leg(g, 8, bodyY + 3, 15, 9);
            }
        }
    }

    private static void leg(PixelGrid g, int x, int fromY, int footY, int shoeX) {
        if (footY - 1 > fromY) {
            g.vline(x, fromY, footY - 1 - fromY, BLUE);
        }
        shoe(g, shoeX, footY);
    }

    /** Кроссовок: (x, y) — левый нижний угол. */
    private static void shoe(PixelGrid g, int x, int y) {
        g.hline(x, y - 1, 3, RED);
        g.px(x + 1, y - 1, WHITE);
        g.hline(x, y, 4, RED_DARK);
        g.hline(x, y, 3, RED);
    }

    private static void drawEye(PixelGrid g, Face face, int lift, boolean tear) {
        int y = 3 + lift;
        switch (face) {
            case NORMAL -> g.vline(10, y + 1, 2, BLACK);
            case SURPRISED -> g.px(10, y + 1, BLACK);
            case BLINK -> {
                g.rect(8, y, 3, 2, BLUE);
                g.px(11, y + 1, BLUE);
                g.hline(8, y + 2, 3, BLACK);
            }
            case HAPPY, EATING -> {
                g.rect(8, y, 3, 2, BLUE);
                g.px(11, y + 1, BLUE);
                g.px(8, y + 2, BLACK);
                g.hline(9, y + 1, 2, BLACK);
                g.px(11, y + 2, BLACK);
            }
            case SAD -> {
                g.hline(8, y, 3, BLUE);
                g.hline(9, y + 2, 2, BLACK);
                if (tear) {
                    g.px(11, y + 3, TEAR);
                    g.px(11, y + 4, TEAR);
                }
            }
        }
    }

    private static void drawMouth(PixelGrid g, Face face, int frame, int lift) {
        int y = 8 + lift;
        switch (face) {
            case EATING -> {
                if (frame == 0) {
                    g.rect(11, y - 1, 2, 2, MOUTH);
                } else {
                    g.hline(11, y, 2, MOUTH);
                }
            }
            case HAPPY -> {
                g.px(10, y - 1, MOUTH);
                g.hline(11, y, 2, MOUTH);
            }
            case SAD -> g.hline(10, y, 2, MOUTH);
            case SURPRISED -> g.px(12, y - 1, MOUTH);
            default -> g.px(11, y, MOUTH);
        }
    }
}
