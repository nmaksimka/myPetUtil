package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

import java.util.Map;

/** Кубический парень в бирюзовой футболке. Нарисован анфас, как в игре. */
final class SteveArt extends CharacterArt {

    private static final int HAIR = rgb(0x3B2716);
    private static final int SKIN = rgb(0xB98A62);
    private static final int SKIN_DARK = rgb(0x9C6E4A);
    private static final int WHITE = rgb(0xFFFFFF);
    private static final int IRIS = rgb(0x4B3FA6);
    private static final int MOUTH = rgb(0x6A402A);
    private static final int SHIRT = rgb(0x2AA5A8);
    private static final int SHIRT_DARK = rgb(0x1E8285);
    private static final int PANTS = rgb(0x3A3FA0);
    private static final int PANTS_DARK = rgb(0x2B2F7E);
    private static final int SHOES = rgb(0x5E5E66);

    private static final Map<Character, Integer> PAL = palette(
            'H', 0x3B2716, 'S', 0xB98A62, 's', 0x9C6E4A, 'n', 0xA0724D);

    private static final String[] HEAD = {
        "....HHHHHHHH....",
        "....HHHHHHHH....",
        "....HSSSSSSH....",
        "....SSSSSSSS....",
        "....SSSSSSSS....",
        "....SSSnnSSS....",
        "....SSSSSSSS....",
        "....sSSSSSSs....",
    };

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int f = pose.frame();
        Anim anim = pose.anim();
        int lift = switch (anim) {
            case HOVER -> f == 0 ? -2 : 0;
            case SAD -> 3;
            case LAND -> 1;
            default -> 0;
        };
        g.stamp(HEAD, 0, lift, PAL);
        drawEyes(g, face, lift + 4, anim == Anim.SAD && f == 1);
        drawMouth(g, face, f, lift + 6);

        int bodyY = 8 + lift;
        g.rect(4, bodyY, 8, 4, SHIRT);
        g.hline(4, bodyY + 3, 8, SHIRT_DARK);

        int leftArm = 0;
        int rightArm = 0;
        boolean armsUp = anim == Anim.HOVER || anim == Anim.DRAG || anim == Anim.FALL;
        if (anim == Anim.WALK) {
            leftArm = (f == 1) ? -1 : (f == 3 ? 1 : 0);
            rightArm = -leftArm;
        }
        if (armsUp) {
            g.rect(2, bodyY - 3, 2, 3, SHIRT).rect(2, bodyY - 5, 2, 2, SKIN);
            g.rect(12, bodyY - 3, 2, 3, SHIRT).rect(12, bodyY - 5, 2, 2, SKIN);
        } else if (anim == Anim.EAT) {
            g.rect(2, bodyY, 2, 2, SHIRT).rect(2, bodyY + 2, 2, 2, SKIN);
            g.rect(12, bodyY, 2, 1, SHIRT).rect(10, bodyY - 1, 2, 2, SKIN);
        } else {
            g.rect(2, bodyY + leftArm, 2, 2, SHIRT).rect(2, bodyY + 2 + leftArm, 2, 2, SKIN);
            g.rect(12, bodyY + rightArm, 2, 2, SHIRT).rect(12, bodyY + 2 + rightArm, 2, 2, SKIN);
        }

        int legY = bodyY + 4;
        if (anim == Anim.SAD) {
            // сидит: ноги вперёд, видны только ступни
            g.rect(4, 15, 3, 1, SHOES);
            g.rect(9, 15, 3, 1, SHOES);
            return;
        }
        int leftLift = 0;
        int rightLift = 0;
        if (anim == Anim.WALK) {
            leftLift = f == 1 ? 1 : 0;
            rightLift = f == 3 ? 1 : 0;
        }
        if (anim == Anim.DRAG || anim == Anim.FALL) {
            leftLift = f == 0 ? 0 : 1;
            rightLift = f == 0 ? 1 : 0;
        }
        leg(g, 4, legY, 15 - leftLift);
        leg(g, 8, legY, 15 - rightLift);
    }

    private static void leg(PixelGrid g, int x, int fromY, int footY) {
        g.rect(x, fromY, 4, footY - fromY, PANTS);
        g.vline(x + 3, fromY, footY - fromY, PANTS_DARK);
        g.hline(x, footY, 4, SHOES);
    }

    private static void drawEyes(PixelGrid g, Face face, int y, boolean tear) {
        switch (face) {
            case NORMAL -> {
                g.px(5, y, WHITE).px(6, y, IRIS);
                g.px(9, y, IRIS).px(10, y, WHITE);
            }
            case SURPRISED -> {
                g.px(5, y, WHITE).px(6, y, IRIS).px(5, y - 1, WHITE).px(6, y - 1, WHITE);
                g.px(9, y, IRIS).px(10, y, WHITE).px(9, y - 1, WHITE).px(10, y - 1, WHITE);
            }
            case BLINK -> {
                g.hline(5, y, 2, SKIN_DARK);
                g.hline(9, y, 2, SKIN_DARK);
            }
            case HAPPY, EATING -> {
                g.px(5, y, HAIR).px(6, y - 1, HAIR);
                g.px(10, y, HAIR).px(9, y - 1, HAIR);
            }
            case SAD -> {
                g.px(5, y, WHITE).px(6, y, IRIS);
                g.px(9, y, IRIS).px(10, y, WHITE);
                g.px(6, y - 1, HAIR).px(9, y - 1, HAIR);
                if (tear) {
                    g.px(5, y + 1, TEAR).px(5, y + 2, TEAR);
                }
            }
        }
    }

    private static void drawMouth(PixelGrid g, Face face, int frame, int y) {
        switch (face) {
            case EATING -> {
                if (frame == 0) {
                    g.rect(6, y, 4, 2, MOUTH);
                } else {
                    g.hline(6, y, 4, MOUTH);
                }
            }
            case HAPPY -> {
                g.px(5, y, MOUTH);
                g.hline(6, y + 1, 4, MOUTH);
                g.px(10, y, MOUTH);
            }
            case SAD -> {
                g.hline(6, y, 4, MOUTH);
                g.px(5, y + 1, MOUTH);
                g.px(10, y + 1, MOUTH);
            }
            case SURPRISED -> g.rect(7, y, 2, 2, MOUTH);
            default -> g.hline(6, y, 4, MOUTH);
        }
    }
}
