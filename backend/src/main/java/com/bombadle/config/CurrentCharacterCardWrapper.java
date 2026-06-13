package com.bombadle.config;

import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.GameMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentCharacterCardWrapper {

    private final Map<GameMode, CharacterCard> currentCards = new ConcurrentHashMap<>();

    public CharacterCard get(GameMode mode) {
        CharacterCard card = currentCards.get(mode);
        if (card == null) {
            throw new IllegalStateException("Character card for mode " + mode + " is not initialized");
        }
        return card;
    }

    public void set(GameMode mode, CharacterCard newInstance) {
        this.currentCards.put(mode, newInstance);
    }
}