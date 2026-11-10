package io.github.nmaksimka.mypetutil.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsStoreTest {

    @TempDir
    Path dir;

    @Test
    void firstLaunchHasOneClaudePet() {
        Settings settings = new SettingsStore(dir.resolve("settings.json")).load();

        assertEquals(1, settings.pets.size());
        assertEquals(PetCharacter.CLAUDE, settings.pets.get(0).character);
        assertEquals(BehaviorMode.WINDOWS, settings.mode);
    }

    @Test
    void savedSettingsAreLoadedBack() {
        SettingsStore store = new SettingsStore(dir.resolve("sub").resolve("settings.json"));
        Settings settings = new Settings();
        settings.mode = BehaviorMode.STAND;
        settings.scale = 5;
        settings.hungerEnabled = true;
        settings.blockedApps.add("game.exe");
        settings.pets.add(new PetEntry(PetCharacter.MINION));

        store.save(settings);
        Settings loaded = store.load();

        assertEquals(BehaviorMode.STAND, loaded.mode);
        assertEquals(5, loaded.scale);
        assertTrue(loaded.hungerEnabled);
        assertTrue(loaded.blockedApps.contains("game.exe"));
        assertEquals(PetCharacter.MINION, loaded.pets.get(0).character);
    }

    @Test
    void brokenFileGivesDefaults() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{ this is not json");

        Settings settings = new SettingsStore(file).load();

        assertEquals(1, settings.pets.size());
    }

    @Test
    void wrongValuesAreFixed() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, """
                {"scale": 100, "speed": 9, "blockedApps": ["Chrome.EXE"],
                 "pets": [{"character": "SONIC", "satiety": 500}, {"character": "UNKNOWN"}]}
                """);

        Settings settings = new SettingsStore(file).load();

        assertEquals(Settings.MAX_SCALE, settings.scale);
        assertEquals(2.0, settings.speed);
        assertTrue(settings.blockedApps.contains("chrome.exe"));
        assertEquals(1, settings.pets.size());
        assertEquals(PetEntry.MAX_SATIETY, settings.pets.get(0).satiety);
    }
}
