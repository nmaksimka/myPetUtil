package io.github.nmaksimka.mypetutil.sprite;

import static io.github.nmaksimka.mypetutil.sprite.PixelGrid.rgb;

/** Оранжевый пупсик из Claude Code: квадратное тельце, ручки по бокам и четыре ножки. */
final class ClaudeArt extends CharacterArt {

    private static final int BODY = rgb(0xD97757);
    private static final int SHADE = rgb(0xB65C3F);
    private static final int EYE = rgb(0x1F1A17);
    private static final int MOUTH = rgb(0x6B2D1D);

    @Override
    protected void draw(PixelGrid g, Pose pose, Face face) {
        int lift = 0;
        int top = 6;
        int bottom = 12;
        int left = 2;
        int width = 12;
        int armY = 9;
        boolean armsUp = false;
        int[] legX = {3, 5, 10, 12};
        int[] legEnd = {15, 15, 15, 15};
        boolean sitting = false;
        int f = pose.frame();

        switch (pose.anim()) {
            case IDLE -> {
                if (f == 1) {
                    top = 7;
                    armY = 10;
                }
            }
            case WALK -> {
                if (f == 1) {
                    legX = new int[] {4, 5, 11, 12};
                    legEnd = new int[] {14, 15, 14, 15};
                    top = 5;
                    bottom = 11;
                    armY = 8;
                } else if (f == 3) {
                    legX = new int[] {3, 6, 10, 13};
                    legEnd = new int[] {15, 14, 15, 14};
                    top = 5;
                    bottom = 11;
                    armY = 8;
                }
            }
            case HOVER -> {
                armsUp = true;
                if (f == 0) {
                    lift = -2;
                }
            }
            case DRAG, FALL -> {
                armsUp = true;
                legX = new int[] {2, 4, 11, 13};
                if (f == 1) {
                    legEnd = new int[] {14, 15, 14, 15};
                }
            }
            case LAND -> {
                top = 8;
                bottom = 13;
                left = 1;
                width = 14;
                armY = 11;
            }
            case EAT -> {
                // стоит ровно, работает только рот
            }
            case SAD -> {
                sitting = true;
                top = 9;
                bottom = 14;
                armY = 12;
            }
        }

        int bodyTop = top + lift;
        int bodyBottom = bottom + lift;
        g.rect(left, bodyTop, width, bodyBottom - bodyTop + 1, BODY);
        g.hline(left, bodyBottom, width, SHADE);

        if (armsUp) {
            g.rect(0, bodyTop, 2, 2, BODY);
            g.rect(14, bodyTop, 2, 2, BODY);
            g.px(0, bodyTop - 1, BODY);
            g.px(15, bodyTop - 1, BODY);
        } else {
            g.rect(0, armY + lift, 2, 2, BODY);
            g.rect(14, armY + lift, 2, 2, BODY);
        }

        if (sitting) {
            g.hline(3, GROUND, 2, SHADE);
            g.hline(11, GROUND, 2, SHADE);
        } else {
            for (int i = 0; i < legX.length; i++) {
                int from = bodyBottom + 1;
                int to = legEnd[i] + lift;
                if (to >= from) {
                    g.vline(legX[i], from, to - from + 1, BODY);
                }
            }
        }

        int eyeY = bodyTop + 2;
        boolean tear = pose.anim() == Anim.SAD && f == 1;
        tallEye(g, face, 5, eyeY, EYE, tear);
        tallEye(g, face, 10, eyeY, EYE, tear);

        if (face == Face.EATING) {
            if (f == 0) {
                g.rect(7, eyeY + 3, 2, 2, MOUTH);
            } else {
                g.hline(7, eyeY + 4, 2, MOUTH);
            }
        } else if (face == Face.SURPRISED) {
            g.px(7, eyeY + 3, MOUTH);
            g.px(8, eyeY + 3, MOUTH);
        }
    }
}
