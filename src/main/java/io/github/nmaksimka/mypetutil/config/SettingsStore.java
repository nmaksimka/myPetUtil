package io.github.nmaksimka.mypetutil.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Читает и сохраняет настройки в %APPDATA%\myPetUtil\settings.json. */
public final class SettingsStore {

    private static final Logger LOG = Logger.getLogger(SettingsStore.class.getName());

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SettingsStore(Path file) {
        this.file = file;
    }

    public static Path defaultDirectory() {
        String override = System.getProperty("mypetutil.home");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("myPetUtil");
    }

    public static SettingsStore createDefault() {
        return new SettingsStore(defaultDirectory().resolve("settings.json"));
    }

    public Path file() {
        return file;
    }

    /**
     * Загружает настройки. Если файла нет или он битый — отдаёт настройки по умолчанию
     * с одним пупсом, чтобы при первом запуске на экране кто-то был.
     */
    public Settings load() {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Settings loaded = gson.fromJson(reader, Settings.class);
                if (loaded != null) {
                    return loaded.normalize();
                }
            } catch (IOException | JsonParseException e) {
                LOG.log(Level.WARNING, "settings.json is broken, using defaults", e);
            }
        }
        Settings settings = new Settings();
        settings.pets.add(new PetEntry(PetCharacter.CLAUDE));
        return settings;
    }

    /** Пишет во временный файл и подменяет — чтобы не оставить обрезанный json при падении. */
    public void save(Settings settings) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                gson.toJson(settings, writer);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot save settings", e);
        }
    }
}
