package com.bombadle.config;

import com.bombadle.entity.CharacterCard;
import lombok.Getter;

public class CurrentCharacterCardWrapper {

    private volatile CharacterCard currentCharacterCard;

    public CurrentCharacterCardWrapper(CharacterCard initialInstance) {
        this.currentCharacterCard = initialInstance;
    }

    public CharacterCard get() {
        return currentCharacterCard;
    }

    public void set(CharacterCard newInstance) {
        this.currentCharacterCard = newInstance;
    }

}
