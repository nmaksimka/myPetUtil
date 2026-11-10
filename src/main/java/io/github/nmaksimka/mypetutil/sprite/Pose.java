package io.github.nmaksimka.mypetutil.sprite;

/**
 * Что нарисовать: анимация, номер кадра и моргает ли питомец.
 * Номер кадра всегда приводится к диапазону анимации.
 */
public record Pose(Anim anim, int frame, boolean blink) {

    public Pose {
        frame = Math.floorMod(frame, anim.frames());
    }

    public static Pose of(Anim anim, int frame) {
        return new Pose(anim, frame, false);
    }
}
