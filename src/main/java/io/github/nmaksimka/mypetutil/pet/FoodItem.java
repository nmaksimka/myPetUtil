package io.github.nmaksimka.mypetutil.pet;

import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.world.Floor;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.World;

/** Еда на экране: её тащат мышкой, она падает и лежит, пока её не съедят. */
public final class FoodItem {

    public enum State { DRAGGED, FALLING, RESTING }

    /** Сколько секунд еда лежит, прежде чем исчезнуть. */
    public static final double LIFETIME = 45;

    private static final double GRAVITY = 2000;

    private final FoodType type;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private State state = State.DRAGGED;
    private String owner;
    private double anchorX;
    private double restTime;
    private boolean eaten;

    public FoodItem(FoodType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public void update(double dt, World world, double pixel, boolean withWindows) {
        double size = size(pixel);
        switch (state) {
            case DRAGGED -> {
            }
            case FALLING -> {
                double oldY = y;
                vy = Math.min(vy + GRAVITY * dt, 2400);
                x += vx * dt;
                y += vy * dt;
                vx *= Math.pow(0.4, dt);
                ScreenInfo screen = world.screenAt(x + size / 2, y - size / 2);
                Rect b = screen.bounds();
                if (x < b.minX() || x + size > b.maxX()) {
                    x = Math.max(b.minX(), Math.min(x, b.maxX() - size));
                    vx = -vx * 0.4;
                }
                Floor floor = world.landing(x + size / 2, oldY, y, withWindows);
                if (floor == null && y > screen.floorY()) {
                    floor = world.screenFloor(screen.index());
                }
                if (floor != null) {
                    land(floor);
                }
            }
            case RESTING -> {
                restTime += dt;
                Floor floor = world.nearestOf(owner, x + size / 2);
                if (floor != null && floor.anchorX() != anchorX) {
                    x += floor.anchorX() - anchorX;
                    anchorX = floor.anchorX();
                }
                floor = world.floorAt(owner, x + size / 2);
                if (floor == null) {
                    drop(0, 0);
                } else {
                    y = floor.y();
                }
            }
        }
    }

    private void land(Floor floor) {
        state = State.RESTING;
        y = floor.y();
        owner = floor.owner();
        anchorX = floor.anchorX();
        vx = 0;
        vy = 0;
        restTime = 0;
    }

    public void moveTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Отпустили мышку — еда падает с заданной скоростью. */
    public void drop(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
        state = State.FALLING;
        owner = null;
    }

    public void markEaten() {
        eaten = true;
    }

    public boolean gone() {
        return eaten || restTime > LIFETIME;
    }

    /** Прозрачность: последние 3 секунды еда тает. */
    public double opacity() {
        double left = LIFETIME - restTime;
        return left >= 3 ? 1 : Math.max(0, left / 3);
    }

    public static double size(double pixel) {
        return FoodType.SIZE * pixel;
    }

    public Rect bounds(double pixel) {
        double size = size(pixel);
        return new Rect(x, y - size, x + size, y);
    }

    public FoodType type() {
        return type;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public State state() {
        return state;
    }

    public String owner() {
        return owner;
    }

    public boolean eaten() {
        return eaten;
    }
}
