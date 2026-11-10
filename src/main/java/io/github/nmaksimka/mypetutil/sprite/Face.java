package io.github.nmaksimka.mypetutil.sprite;

/** Выражение мордочки, вычисляется из позы. */
public enum Face {
    NORMAL,
    BLINK,
    HAPPY,
    SURPRISED,
    SAD,
    EATING;

    public static Face of(Pose pose) {
        return switch (pose.anim()) {
            case IDLE, WALK -> pose.blink() ? BLINK : NORMAL;
            case HOVER -> HAPPY;
            case DRAG, FALL -> SURPRISED;
            case LAND -> BLINK;
            case EAT -> EATING;
            case SAD -> SAD;
        };
    }
}
