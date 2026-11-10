package io.github.nmaksimka.mypetutil.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.WindowInfo;
import io.github.nmaksimka.mypetutil.world.World;
import io.github.nmaksimka.mypetutil.world.WorldBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PetTest {

    private static final double DT = 1.0 / 60;
    private static final ScreenInfo LEFT = new ScreenInfo(0,
            new Rect(0, 0, 1920, 1080), new Rect(0, 0, 1920, 1040), false);
    private static final ScreenInfo RIGHT = new ScreenInfo(1,
            new Rect(1920, 0, 3840, 1080), new Rect(1920, 0, 3840, 1080), false);

    private final Random rnd = new Random(42);

    private static PetRules rules(BehaviorMode mode, boolean hunger) {
        return new PetRules(mode, 3, 1, true, hunger, 1);
    }

    private static World world(List<ScreenInfo> screens, List<WindowInfo> windows, BehaviorMode mode) {
        return WorldBuilder.build(screens, windows, mode == BehaviorMode.WINDOWS, Set.of(), 48, 48);
    }

    private void run(Pet pet, World world, PetRules rules, List<FoodItem> foods, double seconds) {
        for (int i = 0; i < seconds / DT; i++) {
            pet.update(DT, world, rules, foods, rnd);
            foods.forEach(f -> f.update(DT, world, rules.pixel(), rules.walksOnWindows()));
            foods.removeIf(FoodItem::gone);
        }
    }

    @Test
    void fallsAndLandsAboveTaskbar() {
        Pet pet = new Pet(new PetEntry(PetCharacter.CLAUDE), 500, 100);
        World world = world(List.of(LEFT), List.of(), BehaviorMode.STAND);

        run(pet, world, rules(BehaviorMode.STAND, false), new ArrayList<>(), 3);

        assertEquals(1040, pet.y(), 0.001);
        assertEquals("screen:0", pet.owner());
        assertTrue(pet.state().grounded());
    }

    @Test
    void standingPetDoesNotWalk() {
        Pet pet = new Pet(new PetEntry(PetCharacter.CLAUDE), 500, 1040);
        World world = world(List.of(LEFT), List.of(), BehaviorMode.STAND);

        run(pet, world, rules(BehaviorMode.STAND, false), new ArrayList<>(), 20);

        assertEquals(500, pet.x(), 0.001);
    }

    @Test
    void walkingPetStaysOnSingleScreen() {
        Pet pet = new Pet(new PetEntry(PetCharacter.SONIC), 500, 1040);
        World world = world(List.of(LEFT), List.of(), BehaviorMode.DESKTOP);
        PetRules rules = rules(BehaviorMode.DESKTOP, false);

        for (int i = 0; i < 120; i++) {
            run(pet, world, rules, new ArrayList<>(), 1);
            assertTrue(pet.x() >= -48 && pet.x() <= 1920, "x=" + pet.x());
        }
    }

    @Test
    void walkingPetVisitsSecondMonitor() {
        Pet pet = new Pet(new PetEntry(PetCharacter.SONIC), 1800, 1040);
        World world = world(List.of(LEFT, RIGHT), List.of(), BehaviorMode.DESKTOP);
        PetRules rules = rules(BehaviorMode.DESKTOP, false);

        boolean visited = false;
        for (int i = 0; i < 600 && !visited; i++) {
            run(pet, world, rules, new ArrayList<>(), 1);
            visited = "screen:1".equals(pet.owner()) && pet.state() == PetState.WALK;
        }
        assertTrue(visited);
    }

    @Test
    void droppedOnWindowStandsOnItsBottom() {
        WindowInfo window = new WindowInfo(5, new Rect(200, 100, 1200, 700), "chrome.exe", "Chrome");
        World world = world(List.of(LEFT), List.of(window), BehaviorMode.WINDOWS);
        Pet pet = new Pet(new PetEntry(PetCharacter.CLAUDE), 600, 300);
        pet.startDrag();
        pet.release(0, 0);

        run(pet, world, rules(BehaviorMode.WINDOWS, false), new ArrayList<>(), 2);

        assertEquals("win:5", pet.owner());
        assertEquals(700, pet.y(), 0.001);
    }

    @Test
    void petFallsWhenWindowCloses() {
        WindowInfo window = new WindowInfo(5, new Rect(200, 100, 1200, 700), "chrome.exe", "Chrome");
        Pet pet = new Pet(new PetEntry(PetCharacter.CLAUDE), 600, 300);
        PetRules rules = rules(BehaviorMode.WINDOWS, false);
        run(pet, world(List.of(LEFT), List.of(window), BehaviorMode.WINDOWS), rules, new ArrayList<>(), 2);

        run(pet, world(List.of(LEFT), List.of(), BehaviorMode.WINDOWS), rules, new ArrayList<>(), 2);

        assertEquals("screen:0", pet.owner());
        assertEquals(1040, pet.y(), 0.001);
    }

    @Test
    void petMovesTogetherWithWindow() {
        WindowInfo window = new WindowInfo(5, new Rect(200, 100, 1200, 700), "chrome.exe", "Chrome");
        Pet pet = new Pet(new PetEntry(PetCharacter.CLAUDE), 600, 300);
        PetRules rules = new PetRules(BehaviorMode.WINDOWS, 3, 0.000001, true, false, 1);
        run(pet, world(List.of(LEFT), List.of(window), BehaviorMode.WINDOWS), rules, new ArrayList<>(), 2);
        double before = pet.x();

        WindowInfo moved = new WindowInfo(5, new Rect(300, 50, 1300, 650), "chrome.exe", "Chrome");
        run(pet, world(List.of(LEFT), List.of(moved), BehaviorMode.WINDOWS), rules, new ArrayList<>(), DT);

        assertEquals(before + 100, pet.x(), 0.5);
        assertEquals(650, pet.y(), 0.001);
    }

    @Test
    void hungryPetSitsAndEatsOwnFood() {
        PetEntry entry = new PetEntry(PetCharacter.MINION);
        entry.satiety = 0;
        Pet pet = new Pet(entry, 500, 1040);
        World world = world(List.of(LEFT), List.of(), BehaviorMode.DESKTOP);
        PetRules rules = rules(BehaviorMode.DESKTOP, true);
        List<FoodItem> foods = new ArrayList<>();

        run(pet, world, rules, foods, 1);
        assertEquals(PetState.SAD, pet.state());

        FoodItem banana = new FoodItem(FoodType.BANANA, 700, 900);
        banana.drop(0, 0);
        foods.add(banana);
        run(pet, world, rules, foods, 6);

        assertTrue(banana.eaten());
        assertTrue(pet.satiety() > 30);
        assertNotEquals(PetState.SAD, pet.state());
    }

    @Test
    void petRefusesSomeoneElsesFood() {
        Pet pet = new Pet(new PetEntry(PetCharacter.STEVE), 500, 1040);

        boolean eaten = pet.offer(new FoodItem(FoodType.BANANA, 500, 1040));

        assertFalse(eaten);
        assertTrue(pet.questionTimer() > 0);
        assertTrue(pet.offer(new FoodItem(FoodType.GOLDEN_APPLE, 500, 1040)));
    }

    @Test
    void hungerDoesNotGoBelowZero() {
        PetEntry entry = new PetEntry(PetCharacter.GHOST);
        Pet pet = new Pet(entry, 500, 1040);
        World world = world(List.of(LEFT), List.of(), BehaviorMode.STAND);

        run(pet, world, new PetRules(BehaviorMode.STAND, 3, 1, true, true, 50), new ArrayList<>(), 5);

        assertEquals(0, entry.satiety);
        assertEquals(PetState.SAD, pet.state());
    }
}
