package io.github.nmaksimka.mypetutil.config;

import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.util.UUID;

/** Один питомец на экране: кто он и насколько сыт. */
public class PetEntry {

    public static final double MAX_SATIETY = 100;

    public String id = UUID.randomUUID().toString();
    public PetCharacter character = PetCharacter.CLAUDE;
    public double satiety = MAX_SATIETY;

    public PetEntry() {
    }

    public PetEntry(PetCharacter character) {
        this.character = character;
    }
}
