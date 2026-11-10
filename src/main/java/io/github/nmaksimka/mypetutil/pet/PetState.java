package io.github.nmaksimka.mypetutil.pet;

import io.github.nmaksimka.mypetutil.sprite.Anim;

/** Состояние питомца. Каждому соответствует своя анимация. */
public enum PetState {
    IDLE(Anim.IDLE),
    WALK(Anim.WALK),
    /** Мышка над питомцем — он радуется. */
    HOVER(Anim.HOVER),
    /** Радуется после еды. */
    HAPPY(Anim.HOVER),
    DRAG(Anim.DRAG),
    FALL(Anim.FALL),
    LAND(Anim.LAND),
    EAT(Anim.EAT),
    /** Голодный: сидит и грустит. */
    SAD(Anim.SAD),
    /** Уходит за край пола (в окно или за монитор). */
    EXIT(Anim.WALK),
    /** Выходит из-за края на новом полу. */
    ENTER(Anim.WALK);

    private final Anim anim;

    PetState(Anim anim) {
        this.anim = anim;
    }

    public Anim anim() {
        return anim;
    }

    public boolean grounded() {
        return this != DRAG && this != FALL;
    }
}
