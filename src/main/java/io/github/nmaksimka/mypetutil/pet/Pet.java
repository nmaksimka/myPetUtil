package io.github.nmaksimka.mypetutil.pet;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.Pose;
import io.github.nmaksimka.mypetutil.world.Floor;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.World;

import java.util.List;
import java.util.Random;

/**
 * Логика одного питомца без JavaFX: где он стоит, куда идёт и что чувствует.
 * Координаты: x — левый край спрайта, y — линия, на которой стоят ноги.
 */
public final class Pet {

    private static final double GRAVITY = 2200;
    private static final double GHOST_GRAVITY = 700;
    private static final double EAT_SECONDS = 1.6;
    private static final double FOOD_SATIETY = 40;

    private final PetEntry entry;

    private double x;
    private double y;
    private double vx;
    private double vy;
    private int facing = 1;

    private PetState state = PetState.FALL;
    private double stateTime;
    private double animTime;
    private double decisionTimer = 1;
    private double blinkTimer = 3;
    private double blinkLeft;

    private String owner;
    private double anchorX;
    private Double clipMin;
    private Double clipMax;

    private boolean hovered;
    private double heartTimer;
    private double questionTimer;

    public Pet(PetEntry entry, double x, double y) {
        this.entry = entry;
        this.x = x;
        this.y = y;
    }

    public void update(double dt, World world, PetRules rules, List<FoodItem> foods, Random rnd) {
        double size = rules.size();
        stateTime += dt;
        animTime += dt * animRate(rules);
        heartTimer = Math.max(0, heartTimer - dt);
        questionTimer = Math.max(0, questionTimer - dt);
        updateBlink(dt, rnd);

        if (rules.hungerEnabled() && state != PetState.EAT) {
            entry.satiety = Math.max(0, entry.satiety - rules.hungerPerSecond() * dt);
        }

        switch (state) {
            case DRAG -> {
            }
            case FALL -> fall(dt, world, rules, size);
            case EXIT -> exit(dt, world, rules, rnd, size);
            case ENTER -> enter(dt, world, rules, rnd, size);
            default -> grounded(dt, world, rules, foods, rnd, size);
        }
    }

    // ---------- на полу ----------

    private void grounded(double dt, World world, PetRules rules, List<FoodItem> foods, Random rnd, double size) {
        Floor floor = followFloor(world, size);
        if (floor == null) {
            startFall(0, 0);
            return;
        }

        switch (state) {
            case LAND -> {
                if (stateTime > 0.25) {
                    setState(PetState.IDLE);
                }
                return;
            }
            case EAT -> {
                if (stateTime > EAT_SECONDS) {
                    entry.satiety = Math.min(PetEntry.MAX_SATIETY, entry.satiety + FOOD_SATIETY);
                    heartTimer = 1.6;
                    setState(PetState.HAPPY);
                }
                return;
            }
            case HAPPY -> {
                if (stateTime > 0.9) {
                    setState(PetState.IDLE);
                }
                return;
            }
            case HOVER -> {
                heartTimer = Math.max(heartTimer, 0.3);
                if (!hovered) {
                    setState(PetState.IDLE);
                }
                return;
            }
            default -> {
            }
        }

        if (hovered && !isHungry(rules)) {
            setState(PetState.HOVER);
            return;
        }

        FoodItem food = findFood(foods, floor, rules, size);
        if (food != null) {
            goToFood(dt, food, floor, rules, size);
            return;
        }

        if (isHungry(rules)) {
            if (state != PetState.SAD) {
                setState(PetState.SAD);
            }
            return;
        }
        if (state == PetState.SAD) {
            setState(PetState.IDLE);
        }

        if (rules.mode() == BehaviorMode.STAND) {
            if (state != PetState.IDLE) {
                setState(PetState.IDLE);
            }
            decisionTimer -= dt;
            if (decisionTimer <= 0) {
                facing = -facing;
                decisionTimer = 4 + rnd.nextDouble() * 8;
            }
            return;
        }
        wander(dt, world, floor, rules, rnd, size);
    }

    /** Следит, чтобы питомец ехал вместе с окном, если окно двигают. */
    private Floor followFloor(World world, double size) {
        Floor any = world.nearestOf(owner, x + size / 2);
        if (any == null) {
            return null;
        }
        if (any.anchorX() != anchorX) {
            x += any.anchorX() - anchorX;
            anchorX = any.anchorX();
        }
        Floor floor = world.floorAt(owner, x + size / 2);
        if (floor != null) {
            y = floor.y();
            x = clamp(x, floor.minX(), floor.maxX() - size);
        }
        return floor;
    }

    private void wander(double dt, World world, Floor floor, PetRules rules, Random rnd, double size) {
        decisionTimer -= dt;
        if (state == PetState.IDLE) {
            if (decisionTimer <= 0) {
                setState(PetState.WALK);
                if (rnd.nextDouble() < 0.4) {
                    facing = -facing;
                }
                decisionTimer = 2 + rnd.nextDouble() * 7;
            }
            return;
        }
        if (state != PetState.WALK) {
            setState(PetState.WALK);
        }
        x += facing * walkSpeed(rules) * dt;
        boolean atRight = facing > 0 && x + size >= floor.maxX();
        boolean atLeft = facing < 0 && x <= floor.minX();
        if (atRight || atLeft) {
            if (!exitTargets(world, floor, rules, size).isEmpty() && rnd.nextDouble() < 0.65) {
                setState(PetState.EXIT);
                clipMin = floor.minX();
                clipMax = floor.maxX();
                return;
            }
            x = clamp(x, floor.minX(), floor.maxX() - size);
            facing = -facing;
        }
        if (decisionTimer <= 0) {
            setState(PetState.IDLE);
            decisionTimer = 1.5 + rnd.nextDouble() * 4;
        }
    }

    // ---------- переходы между окнами и мониторами ----------

    private List<Floor> exitTargets(World world, Floor from, PetRules rules, double size) {
        if (rules.mode() == BehaviorMode.STAND) {
            return List.of();
        }
        return world.exits(from.owner(), rules.walksOnWindows(), size * 1.5).stream()
                .filter(f -> rules.multiMonitor() || f.screen() == from.screen())
                .toList();
    }

    private void exit(double dt, World world, PetRules rules, Random rnd, double size) {
        Floor floor = world.nearestOf(owner, x + size / 2);
        if (floor == null) {
            startFall(0, 0);
            return;
        }
        y = floor.y();
        clipMin = floor.minX();
        clipMax = floor.maxX();
        x += facing * walkSpeed(rules) * dt;
        boolean out = facing > 0 ? x >= floor.maxX() : x + size <= floor.minX();
        if (!out) {
            return;
        }
        List<Floor> targets = exitTargets(world, floor, rules, size);
        Floor target = targets.isEmpty() ? floor : targets.get(rnd.nextInt(targets.size()));
        boolean fromLeft = rnd.nextBoolean();
        owner = target.owner();
        anchorX = target.anchorX();
        y = target.y();
        if (fromLeft) {
            x = target.minX() - size;
            facing = 1;
        } else {
            x = target.maxX();
            facing = -1;
        }
        clipMin = target.minX();
        clipMax = target.maxX();
        setState(PetState.ENTER);
    }

    private void enter(double dt, World world, PetRules rules, Random rnd, double size) {
        Floor floor = world.nearestOf(owner, x + size / 2);
        if (floor == null) {
            clipMin = null;
            clipMax = null;
            startFall(0, 0);
            return;
        }
        y = floor.y();
        clipMin = floor.minX();
        clipMax = floor.maxX();
        x += facing * walkSpeed(rules) * dt;
        if (x >= floor.minX() && x + size <= floor.maxX()) {
            clipMin = null;
            clipMax = null;
            setState(PetState.WALK);
            decisionTimer = 1.5 + rnd.nextDouble() * 3;
        }
    }

    // ---------- полёт ----------

    private void fall(double dt, World world, PetRules rules, double size) {
        boolean floats = character().floats();
        double oldY = y;
        vy = Math.min(vy + (floats ? GHOST_GRAVITY : GRAVITY) * dt, floats ? 300 : 2600);
        x += vx * dt;
        y += vy * dt;
        vx *= Math.pow(floats ? 0.2 : 0.5, dt);

        double cx = x + size / 2;
        ScreenInfo screen = world.screenAt(cx, y - size / 2);
        Rect b = screen.bounds();
        if (cx < b.minX() || cx > b.maxX()) {
            x = clamp(cx, b.minX() + 1, b.maxX() - 1) - size / 2;
            vx = -vx * 0.4;
            cx = x + size / 2;
        }
        if (y - size < b.minY()) {
            y = b.minY() + size;
            vy = Math.max(vy, 0);
        }
        if (vy < 0) {
            return;
        }
        Floor floor = world.landing(cx, oldY, y, rules.walksOnWindows());
        if (floor == null && y > screen.floorY()) {
            floor = world.screenFloor(screen.index());
        }
        if (floor != null) {
            double impact = vy;
            y = floor.y();
            owner = floor.owner();
            anchorX = floor.anchorX();
            x = clamp(x, floor.minX(), floor.maxX() - size);
            vx = 0;
            vy = 0;
            setState(!floats && impact > 700 ? PetState.LAND : PetState.IDLE);
            decisionTimer = 0.8;
        }
    }

    private void startFall(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
        owner = null;
        clipMin = null;
        clipMax = null;
        setState(PetState.FALL);
    }

    // ---------- еда ----------

    private FoodItem findFood(List<FoodItem> foods, Floor floor, PetRules rules, double size) {
        FoodItem best = null;
        double bestDistance = Double.MAX_VALUE;
        double cx = x + size / 2;
        for (FoodItem food : foods) {
            if (food.eaten() || food.type() != character().food() || food.state() != FoodItem.State.RESTING
                    || !floor.owner().equals(food.owner())) {
                continue;
            }
            double foodCx = food.x() + FoodItem.size(rules.pixel()) / 2;
            double distance = Math.abs(foodCx - cx);
            if (rules.mode() == BehaviorMode.STAND && distance > size * 2.5) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = food;
            }
        }
        return best;
    }

    private void goToFood(double dt, FoodItem food, Floor floor, PetRules rules, double size) {
        double foodCx = food.x() + FoodItem.size(rules.pixel()) / 2;
        double cx = x + size / 2;
        if (Math.abs(foodCx - cx) < size * 0.35) {
            eat(food);
            return;
        }
        facing = foodCx > cx ? 1 : -1;
        if (state != PetState.WALK) {
            setState(PetState.WALK);
        }
        x += facing * walkSpeed(rules) * 1.4 * dt;
        x = clamp(x, floor.minX(), floor.maxX() - size);
    }

    /**
     * Дали еду прямо в руки. Если это его еда — ест, иначе удивляется.
     *
     * @return true, если еду съели
     */
    public boolean offer(FoodItem food) {
        if (food.type() != character().food() || state == PetState.DRAG) {
            questionTimer = 1.6;
            return false;
        }
        eat(food);
        return true;
    }

    private void eat(FoodItem food) {
        food.markEaten();
        if (state == PetState.FALL || state == PetState.DRAG) {
            // ест в полёте — ничего страшного, просто сытость
            entry.satiety = Math.min(PetEntry.MAX_SATIETY, entry.satiety + FOOD_SATIETY);
            heartTimer = 1.6;
            return;
        }
        clipMin = null;
        clipMax = null;
        setState(PetState.EAT);
    }

    // ---------- мышка ----------

    public void startDrag() {
        hovered = false;
        clipMin = null;
        clipMax = null;
        owner = null;
        vx = 0;
        vy = 0;
        setState(PetState.DRAG);
    }

    public void dragTo(double x, double y, double velocityX) {
        this.x = x;
        this.y = y;
        this.vx = velocityX;
    }

    public void release(double vx, double vy) {
        startFall(clamp(vx, -2500, 2500), clamp(vy, -2500, 1500));
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    // ---------- вспомогательное ----------

    private boolean isHungry(PetRules rules) {
        return rules.hungerEnabled() && entry.satiety <= 0;
    }

    private double walkSpeed(PetRules rules) {
        return PetRules.WALK_PIXELS_PER_SECOND * rules.pixel() * rules.speed() * character().speedFactor();
    }

    private double animRate(PetRules rules) {
        return state.anim() == Anim.WALK ? rules.speed() * character().speedFactor() : 1;
    }

    private void updateBlink(double dt, Random rnd) {
        if (blinkLeft > 0) {
            blinkLeft -= dt;
            return;
        }
        blinkTimer -= dt;
        if (blinkTimer <= 0) {
            blinkLeft = 0.14;
            blinkTimer = 2 + rnd.nextDouble() * 4;
        }
    }

    private void setState(PetState next) {
        if (state != next) {
            state = next;
            stateTime = 0;
            animTime = 0;
        }
    }

    private static double clamp(double v, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    // ---------- для отрисовки и тестов ----------

    public Pose pose() {
        Anim anim = state.anim();
        int frame = (int) (animTime / anim.frameSeconds());
        boolean blink = blinkLeft > 0 && (anim == Anim.IDLE || anim == Anim.WALK);
        return new Pose(anim, frame, blink);
    }

    public Rect bounds(double size) {
        return new Rect(x, y - size, x + size, y);
    }

    public PetEntry entry() {
        return entry;
    }

    public PetCharacter character() {
        return entry.character;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double vx() {
        return vx;
    }

    public int facing() {
        return facing;
    }

    public PetState state() {
        return state;
    }

    public String owner() {
        return owner;
    }

    public Double clipMin() {
        return clipMin;
    }

    public Double clipMax() {
        return clipMax;
    }

    public boolean hovered() {
        return hovered;
    }

    public double heartTimer() {
        return heartTimer;
    }

    public double questionTimer() {
        return questionTimer;
    }

    public double satiety() {
        return entry.satiety;
    }
}
