package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.PreviousCharacterCardDto;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CurrentCardStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CurrentCardStateService {

    /**
     * The base "Kapitan Bomba" character card, seeded by Flyway (V9) and guaranteed to exist.
     * Used to backfill previousCards on a fresh database (see {@link #updateCurrentState}).
     */
    private static final long FALLBACK_CHARACTER_CARD_ID = 1L;

    private final CurrentCardStateRepository repo;
    private final CurrentGameStateWrapper currentGameStateWrapper;
    private final CharacterCardService characterCardService;

    @PostConstruct
    public void setUpCurrentCardIfStateExists() {
        repo.findById(1).ifPresent(state -> {
            state.getCurrentCards().forEach(currentGameStateWrapper::set);

            if (state.getCurrentQuote() != null) {
                currentGameStateWrapper.setQuote(state.getCurrentQuote());
            }
        });
    }

    public CurrentCardState getCurrentCardState() {
        return repo.findById(1).orElseThrow(() -> new IllegalStateException("Global card state not found"));
    }

    /**
     * Changes exactly once per day, at {@link #updateCurrentState}, which evicts this cache.
     */
    @Cacheable(value = "previous-character-card", key = "#mode")
    public Optional<PreviousCharacterCardDto> getPreviousCharacterCard(GameMode mode) {
        CharacterCard prevCard = getCurrentCardState().getPreviousCards().get(mode);

        if (prevCard == null) {
            return Optional.empty();
        }

        return Optional.of(PreviousCharacterCardDto.builder()
                .name(prevCard.getName())
                .imageSrc(prevCard.getImageSrc())
                .build());
    }

    @Transactional
    @CacheEvict(value = "previous-character-card", allEntries = true)
    public void updateCurrentState(Map<GameMode, CharacterCard> newCards, Quote newQuote) {
        CurrentCardState state = repo.findById(1).orElseGet(() -> {
            CurrentCardState newState = new CurrentCardState();
            newState.setId(1);
            return newState;
        });

        state.getPreviousCards().putAll(state.getCurrentCards());
        state.setPreviousQuote(state.getCurrentQuote());

        // On a fresh boot there is no current card to carry forward, so previousCards would be
        // left without an entry for these modes and /previous-character-card would surface a
        // broken/empty state. Default any missing entry to the base "Kapitan Bomba" card (id=1).
        // Scope is previousCards only; previousQuote's null-handling is intentionally left as-is.
        applyPreviousCardFallback(state, newCards.keySet());

        state.getCurrentCards().putAll(newCards);
        state.setCurrentQuote(newQuote);

        repo.save(state);
    }

    private void applyPreviousCardFallback(CurrentCardState state, Set<GameMode> modes) {
        CharacterCard fallbackCard = null;
        for (GameMode mode : modes) {
            if (state.getPreviousCards().get(mode) == null) {
                if (fallbackCard == null) {
                    fallbackCard = characterCardService.findCharacterCardById(FALLBACK_CHARACTER_CARD_ID)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Fallback character card id=" + FALLBACK_CHARACTER_CARD_ID + " not found"));
                }
                state.getPreviousCards().put(mode, fallbackCard);
            }
        }
    }
}