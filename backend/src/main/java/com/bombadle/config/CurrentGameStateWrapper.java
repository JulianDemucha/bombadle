package com.bombadle.config;

import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentGameStateWrapper {

    private final Map<GameMode, CharacterCard> currentCards = new ConcurrentHashMap<>();

    private Quote currentQuote;

    public CharacterCard getCard(GameMode mode) {
        if(mode == GameMode.QUOTES_STAGE_1)
            throw new IllegalStateException("Quotes stage 1 is gettable only by getQuote()");

        CharacterCard card = currentCards.get(mode);
        if (card == null) {
            throw new IllegalStateException("Character card for mode " + mode + " is not initialized");
        }
        return card;
    }

    public Quote getQuote() {
        if (currentQuote == null) {
            throw new IllegalStateException("Quote for stage 1 is not initialized");
        }
        return currentQuote;
    }

    public void set(GameMode mode, CharacterCard newInstance) {
        this.currentCards.put(mode, newInstance);
    }

    public void setQuote(Quote newInstance) {
        this.currentQuote = newInstance;
    }

    public void clear() {
        currentQuote = null;
        currentCards.clear();
    }
}