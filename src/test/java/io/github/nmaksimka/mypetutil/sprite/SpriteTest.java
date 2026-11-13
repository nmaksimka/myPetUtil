package io.github.nmaksimka.mypetutil.sprite;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SpriteTest {

    @ParameterizedTest
    @EnumSource(PetCharacter.class)
    void everyFrameIsDrawn(PetCharacter character) {
        for (Anim anim : Anim.values()) {
            for (int f = 0; f < anim.frames(); f++) {
                PixelGrid frame = character.art().frame(Pose.of(anim, f));
                assertTrue(opaque(frame) > 30, character + " " + anim + " " + f + " is almost empty");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(PetCharacter.class)
    void standingPetTouchesGround(PetCharacter character) {
        if (character.floats()) {
            return;
        }
        PixelGrid frame = character.art().frame(Pose.of(Anim.IDLE, 0));
        boolean touches = false;
        for (int x = 0; x < frame.width(); x++) {
            touches |= frame.isOpaque(x, CharacterArt.GROUND);
        }
        assertTrue(touches, character + " floats above ground");
    }

    @Test
    void framesAreCached() {
        Pose pose = Pose.of(Anim.WALK, 2);
        assertSame(PetCharacter.SAKURA.art().frame(pose), PetCharacter.SAKURA.art().frame(pose));
    }

    @Test
    void frameNumberWrapsAround() {
        assertTrue(Pose.of(Anim.WALK, 5).frame() == 1);
        assertTrue(Pose.of(Anim.IDLE, -1).frame() == 1);
    }

    @Test
    void everyFoodIsDrawn() {
        for (FoodType food : FoodType.values()) {
            assertTrue(opaque(food.grid()) > 15, food + " is almost empty");
        }
    }

    @Test
    void unknownPaletteCharFails() {
        PixelGrid grid = new PixelGrid(2, 1);
        assertThrows(IllegalArgumentException.class, () -> grid.stamp(new String[] {"ab"}, 0, 0, Map.of('a', 1)));
    }

    private static int opaque(PixelGrid grid) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.isOpaque(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }
}
