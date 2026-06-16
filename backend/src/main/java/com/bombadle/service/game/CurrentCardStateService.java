package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CurrentCardStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentCardStateService {

    private final CurrentCardStateRepository repo;
    private final CurrentGameStateWrapper currentGameStateWrapper;

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

    @Transactional
    public void updateCurrentState(Map<GameMode, CharacterCard> newCards, Quote newQuote) {
        CurrentCardState state = repo.findById(1).orElseGet(() -> {
            CurrentCardState newState = new CurrentCardState();
            newState.setId(1);
            return newState;
        });

        state.getPreviousCards().putAll(state.getCurrentCards());
        state.setPreviousQuote(state.getCurrentQuote());

        state.getCurrentCards().putAll(newCards);
        state.setCurrentQuote(newQuote);

        repo.save(state);
    }
}