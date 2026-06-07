package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.repository.CurrentCardStateRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentCardStateServiceTest {

    @Mock
    private CurrentCardStateRepository repo;

    @Mock
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;

    @InjectMocks
    private CurrentCardStateService currentCardStateService;

    @Nested
    class SetUpCurrentCardIfStateExists {

        @Test
        void stateExists_setsCard() {
            CharacterCard card = CharacterCard.builder().build();
            CurrentCardState state = new CurrentCardState();
            state.setCurrentCharacter(card);

            when(repo.findById(1)).thenReturn(Optional.of(state));

            currentCardStateService.setUpCurrentCardIfStateExists();

            verify(currentCharacterCardWrapper, times(1)).set(card);
        }

        @Test
        void stateDoesNotExist_doesNothing() {
            when(repo.findById(1)).thenReturn(Optional.empty());

            currentCardStateService.setUpCurrentCardIfStateExists();

            verify(currentCharacterCardWrapper, never()).set(any());
        }
    }

    @Nested
    class GetCurrentCardState {

        @Test
        void stateExists_returnsState() {
            CurrentCardState expectedState = new CurrentCardState();
            when(repo.findById(1)).thenReturn(Optional.of(expectedState));

            CurrentCardState actualState = currentCardStateService.getCurrentCardState();

            assertEquals(expectedState, actualState);
        }

        @Test
        void stateDoesNotExist_throwsException() {
            when(repo.findById(1)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> currentCardStateService.getCurrentCardState());
        }
    }

    @Nested
    class UpdateCurrentCard {

        @Test
        void stateExists_updatesExistingState() {
            CharacterCard oldCard = CharacterCard.builder().build();
            CharacterCard newCard = CharacterCard.builder().build();

            CurrentCardState existingState = new CurrentCardState();
            existingState.setCurrentCharacter(oldCard);

            when(repo.findById(1)).thenReturn(Optional.of(existingState));

            currentCardStateService.updateCurrentCard(newCard);

            assertEquals(oldCard, existingState.getPreviousCharacter());
            assertEquals(newCard, existingState.getCurrentCharacter());

            // Verify dirty checking is used (no explicit save)
            verify(repo, never()).save(any());
        }

        @Test
        void stateDoesNotExist_createsAndSavesNewState() {
            CharacterCard newCard = CharacterCard.builder().build();
            when(repo.findById(1)).thenReturn(Optional.empty());

            currentCardStateService.updateCurrentCard(newCard);

            ArgumentCaptor<CurrentCardState> stateCaptor = ArgumentCaptor.forClass(CurrentCardState.class);
            verify(repo, times(1)).save(stateCaptor.capture());

            CurrentCardState savedState = stateCaptor.getValue();
            assertEquals(1, savedState.getId());
            assertEquals(newCard, savedState.getCurrentCharacter());
            assertEquals(newCard, savedState.getPreviousCharacter());
        }
    }
}