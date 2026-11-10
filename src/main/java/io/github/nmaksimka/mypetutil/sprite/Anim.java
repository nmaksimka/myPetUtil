package io.github.nmaksimka.mypetutil.sprite;

/** Какая анимация сейчас играет у питомца. */
public enum Anim {
    IDLE(2, 0.6),
    WALK(4, 0.13),
    HOVER(2, 0.18),
    DRAG(2, 0.2),
    FALL(1, 1),
    LAND(1, 1),
    EAT(2, 0.2),
    SAD(2, 0.7);

    private final int frames;
    private final double frameSeconds;

    Anim(int frames, double frameSeconds) {
        this.frames = frames;
        this.frameSeconds = frameSeconds;
    }

    public int frames() {
        return frames;
    }

    public double frameSeconds() {
        return frameSeconds;
    }
}
