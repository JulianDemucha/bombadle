package com.bombadle.service.game;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.CurrentCardStateRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentCardStateServiceTest {

    @Mock
    private CurrentCardStateRepository repo;

    @Mock
    private CurrentGameStateWrapper currentGameStateWrapper;

    @Mock
    private CharacterCardService characterCardService;

    @InjectMocks
    private CurrentCardStateService currentCardStateService;

    @Nested
    class SetUpCurrentCardIfStateExistsTests {

        @Test
        void setUpCurrentCardIfStateExists_stateExists_setsCardsAndQuoteInWrapper() {
            // ARRANGE
            CurrentCardState state = new CurrentCardState();
            CharacterCard card = mock(CharacterCard.class);
            Quote quote = mock(Quote.class);
            state.getCurrentCards().put(GameMode.CLASSIC, card);
            state.setCurrentQuote(quote);

            when(repo.findById(1)).thenReturn(Optional.of(state));

            // ACT
            currentCardStateService.setUpCurrentCardIfStateExists();

            // ASSERT
            verify(currentGameStateWrapper).set(GameMode.CLASSIC, card);
            verify(currentGameStateWrapper).setQuote(quote);
        }

        @Test
        void setUpCurrentCardIfStateExists_stateDoesNotExist_doesNothing() {
            // ARRANGE
            when(repo.findById(1)).thenReturn(Optional.empty());

            // ACT
            currentCardStateService.setUpCurrentCardIfStateExists();

            // ASSERT
            verifyNoInteractions(currentGameStateWrapper);
        }
    }

    @Nested
    class GetCurrentCardStateTests {

        @Test
        void getCurrentCardState_stateExists_returnsState() {
            // ARRANGE
            CurrentCardState expectedState = new CurrentCardState();
            when(repo.findById(1)).thenReturn(Optional.of(expectedState));

            // ACT
            CurrentCardState actualState = currentCardStateService.getCurrentCardState();

            // ASSERT
            assertEquals(expectedState, actualState);
        }

        @Test
        void getCurrentCardState_stateDoesNotExist_throwsException() {
            // ARRANGE
            when(repo.findById(1)).thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThrows(IllegalStateException.class, () -> currentCardStateService.getCurrentCardState());
        }
    }

    @Nested
    class UpdateCurrentStateTests {

        @Test
        void updateCurrentState_stateExists_updatesAndSavesState() {
            // ARRANGE
            CharacterCard oldCard = mock(CharacterCard.class);
            CharacterCard newCard = mock(CharacterCard.class);
            Map<GameMode, CharacterCard> newCards = Map.of(GameMode.CLASSIC, newCard);

            Quote oldQuote = mock(Quote.class);
            Quote newQuote = mock(Quote.class);

            CurrentCardState existingState = new CurrentCardState();
            existingState.getCurrentCards().put(GameMode.CLASSIC, oldCard);
            existingState.setCurrentQuote(oldQuote);

            when(repo.findById(1)).thenReturn(Optional.of(existingState));

            // ACT
            currentCardStateService.updateCurrentState(newCards, newQuote);

            // ASSERT
            assertEquals(oldCard, existingState.getPreviousCards().get(GameMode.CLASSIC));
            assertEquals(newCard, existingState.getCurrentCards().get(GameMode.CLASSIC));
            assertEquals(oldQuote, existingState.getPreviousQuote());
            assertEquals(newQuote, existingState.getCurrentQuote());
            verify(repo).save(existingState);
        }

        @Test
        void updateCurrentState_stateDoesNotExist_createsNewAndSavesState() {
            // ARRANGE
            CharacterCard newCard = mock(CharacterCard.class);
            Map<GameMode, CharacterCard> newCards = Map.of(GameMode.CLASSIC, newCard);
            Quote newQuote = mock(Quote.class);

            CharacterCard fallbackCard = mock(CharacterCard.class);

            when(repo.findById(1)).thenReturn(Optional.empty());
            when(characterCardService.findCharacterCardById(1L)).thenReturn(Optional.of(fallbackCard));

            // ACT
            currentCardStateService.updateCurrentState(newCards, newQuote);

            // ASSERT
            ArgumentCaptor<CurrentCardState> stateCaptor = ArgumentCaptor.forClass(CurrentCardState.class);
            verify(repo).save(stateCaptor.capture());

            CurrentCardState savedState = stateCaptor.getValue();
            assertEquals(1, savedState.getId());
            assertEquals(newCard, savedState.getCurrentCards().get(GameMode.CLASSIC));
            assertEquals(newQuote, savedState.getCurrentQuote());
            // On a fresh DB previousCards has no card to carry forward, so it falls back to id=1.
            assertEquals(fallbackCard, savedState.getPreviousCards().get(GameMode.CLASSIC));
        }

        @Test
        void updateCurrentState_stateDoesNotExist_missingModeFallsBackToBaseCard() {
            // ARRANGE
            CharacterCard classicCard = mock(CharacterCard.class);
            CharacterCard quotesCard = mock(CharacterCard.class);
            Map<GameMode, CharacterCard> newCards = Map.of(
                    GameMode.CLASSIC, classicCard,
                    GameMode.QUOTES_STAGE_2, quotesCard);
            Quote newQuote = mock(Quote.class);

            CharacterCard fallbackCard = mock(CharacterCard.class);

            when(repo.findById(1)).thenReturn(Optional.empty());
            when(characterCardService.findCharacterCardById(1L)).thenReturn(Optional.of(fallbackCard));

            // ACT
            currentCardStateService.updateCurrentState(newCards, newQuote);

            // ASSERT: every played mode gets a non-null previous card (the id=1 fallback).
            ArgumentCaptor<CurrentCardState> stateCaptor = ArgumentCaptor.forClass(CurrentCardState.class);
            verify(repo).save(stateCaptor.capture());

            CurrentCardState savedState = stateCaptor.getValue();
            assertEquals(fallbackCard, savedState.getPreviousCards().get(GameMode.CLASSIC));
            assertEquals(fallbackCard, savedState.getPreviousCards().get(GameMode.QUOTES_STAGE_2));
        }
    }
}