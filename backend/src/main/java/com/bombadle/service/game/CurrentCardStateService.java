package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.repository.CurrentCardStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentCardStateService {

    private final CurrentCardStateRepository repo;
    private final CurrentCharacterCardWrapper currentCharacterCardWrapper;

    @PostConstruct
    public void setUpCurrentCardIfStateExists() {
        repo.findById(1).ifPresent(
                currentCardState -> currentCharacterCardWrapper.set(currentCardState.getCurrentCharacter())
        );
    }

    public CurrentCardState getCurrentCardState() {
        return repo.findById(1).orElseThrow(); // todo custom exception
    }

    @Transactional
    public void updateCurrentCard(CharacterCard newCard) {
        Optional<CurrentCardState> stateOpt = repo.findById(1);

        if (stateOpt.isPresent()) {
            CurrentCardState existingState = stateOpt.get();
            existingState.setPreviousCharacter(existingState.getCurrentCharacter());
            existingState.setCurrentCharacter(newCard);

        } else {
            // fallback
            CurrentCardState newState = new CurrentCardState();
            newState.setId(1);
            newState.setCurrentCharacter(newCard);
            newState.setPreviousCharacter(newCard);

            repo.save(newState);
        }
    }
}