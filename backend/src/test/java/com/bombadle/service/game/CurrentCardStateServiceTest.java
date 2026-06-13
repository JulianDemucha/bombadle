package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CurrentCardStateRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    class SetUpCurrentCardIfStateExistsTests {

        @Test
        void setUpCurrentCardIfStateExists_stateExists_setsCardsInWrapper() {
            // Arrange
            CurrentCardState state = new CurrentCardState();
            CharacterCard card = mock(CharacterCard.class);
            state.getCurrentCards().put(GameMode.CLASSIC, card);

            when(repo.findById(1)).thenReturn(Optional.of(state));

            // Act
            currentCardStateService.setUpCurrentCardIfStateExists();

            // Assert
            verify(currentCharacterCardWrapper).set(GameMode.CLASSIC, card);
        }

        @Test
        void setUpCurrentCardIfStateExists_stateDoesNotExist_doesNothing() {
            // Arrange
            when(repo.findById(1)).thenReturn(Optional.empty());

            // Act
            currentCardStateService.setUpCurrentCardIfStateExists();

            // Assert
            verifyNoInteractions(currentCharacterCardWrapper);
        }
    }

    @Nested
    class GetCurrentCardStateTests {

        @Test
        void getCurrentCardState_stateExists_returnsState() {
            // Arrange
            CurrentCardState expectedState = new CurrentCardState();
            when(repo.findById(1)).thenReturn(Optional.of(expectedState));

            // Act
            CurrentCardState actualState = currentCardStateService.getCurrentCardState();

            // Assert
            assertEquals(expectedState, actualState);
        }

        @Test
        void getCurrentCardState_stateDoesNotExist_throwsException() {
            // Arrange
            when(repo.findById(1)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(IllegalStateException.class, () -> currentCardStateService.getCurrentCardState());
        }
    }

    @Nested
    class UpdateCurrentCardsTests {

        @Test
        void updateCurrentCards_stateExists_updatesAndSavesState() {
            // Arrange
            CharacterCard oldCard = mock(CharacterCard.class);
            CharacterCard newCard = mock(CharacterCard.class);
            Map<GameMode, CharacterCard> newCards = Map.of(GameMode.CLASSIC, newCard);

            CurrentCardState existingState = new CurrentCardState();
            existingState.getCurrentCards().put(GameMode.CLASSIC, oldCard);

            when(repo.findById(1)).thenReturn(Optional.of(existingState));

            // Act
            currentCardStateService.updateCurrentCards(newCards);

            // Assert
            assertEquals(oldCard, existingState.getPreviousCards().get(GameMode.CLASSIC));
            assertEquals(newCard, existingState.getCurrentCards().get(GameMode.CLASSIC));
            verify(repo).save(existingState);
        }

        @Test
        void updateCurrentCards_stateDoesNotExist_createsNewAndSavesState() {
            // Arrange
            CharacterCard newCard = mock(CharacterCard.class);
            Map<GameMode, CharacterCard> newCards = Map.of(GameMode.CLASSIC, newCard);

            when(repo.findById(1)).thenReturn(Optional.empty());

            // Act
            currentCardStateService.updateCurrentCards(newCards);

            // Assert
            ArgumentCaptor<CurrentCardState> stateCaptor = ArgumentCaptor.forClass(CurrentCardState.class);
            verify(repo).save(stateCaptor.capture());

            CurrentCardState savedState = stateCaptor.getValue();
            assertEquals(1, savedState.getId());
            assertEquals(newCard, savedState.getCurrentCards().get(GameMode.CLASSIC));
        }
    }
}