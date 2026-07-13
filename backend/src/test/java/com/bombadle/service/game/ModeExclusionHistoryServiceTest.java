package com.bombadle.service.game;

import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.ModeExclusionHistory;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.ModeExclusionHistoryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModeExclusionHistoryServiceTest {

    @Mock
    private ModeExclusionHistoryRepository repo;
    @Mock
    private CharacterCardService characterCardService;
    @Mock
    private QuoteService quoteService;

    @InjectMocks
    private ModeExclusionHistoryService modeExclusionHistoryService;

    @Nested
    class PickCardForModeTests {

        @Test
        void pickCardForMode_historyExistsAndDrawSucceeds_excludesUnionAndRecordsPick() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.CLASSIC)
                    .excludedIds(new HashSet<>(Set.of(10L, 20L)))
                    .build();
            when(repo.findByGameMode(GameMode.CLASSIC)).thenReturn(Optional.of(history));

            CharacterCard pickedCard = mock(CharacterCard.class);
            when(pickedCard.getId()).thenReturn(99L);
            when(characterCardService.findRandomCardExcluding(anyList())).thenReturn(pickedCard);

            // Act
            CharacterCard result = modeExclusionHistoryService.pickCardForMode(GameMode.CLASSIC, List.of(30L), null);

            // Assert
            assertEquals(pickedCard, result);

            ArgumentCaptor<List<Long>> excludedCaptor = ArgumentCaptor.forClass(List.class);
            verify(characterCardService).findRandomCardExcluding(excludedCaptor.capture());
            assertEquals(Set.of(10L, 20L, 30L), new HashSet<>(excludedCaptor.getValue()));

            verify(characterCardService, never()).findRandomCard();
            verify(repo).save(history);
            assertEquals(Set.of(10L, 20L, 99L), history.getExcludedIds());
        }

        @Test
        void pickCardForMode_noHistoryRowYetAndNoSameDayExclusions_drawsUnfiltered() {
            // Arrange
            when(repo.findByGameMode(GameMode.IMAGES)).thenReturn(Optional.empty());

            CharacterCard pickedCard = mock(CharacterCard.class);
            when(pickedCard.getId()).thenReturn(5L);
            when(characterCardService.findRandomCard()).thenReturn(pickedCard);

            // Act
            CharacterCard result = modeExclusionHistoryService.pickCardForMode(GameMode.IMAGES, List.of(), null);

            // Assert
            assertEquals(pickedCard, result);
            verify(characterCardService).findRandomCard();
            // An empty NOT IN (...) list is invalid SQL, so the excluding query must never be called with one.
            verify(characterCardService, never()).findRandomCardExcluding(any());

            ArgumentCaptor<ModeExclusionHistory> historyCaptor = ArgumentCaptor.forClass(ModeExclusionHistory.class);
            verify(repo).save(historyCaptor.capture());
            ModeExclusionHistory saved = historyCaptor.getValue();
            assertEquals(GameMode.IMAGES, saved.getGameMode());
            assertEquals(Set.of(5L), saved.getExcludedIds());
        }

        @Test
        void pickCardForMode_poolExhausted_resetsToMostRecentPickAndRedraws() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.CLASSIC)
                    .excludedIds(new HashSet<>(Set.of(1L, 2L, 3L)))
                    .build();
            when(repo.findByGameMode(GameMode.CLASSIC)).thenReturn(Optional.of(history));

            CharacterCard mostRecentCard = mock(CharacterCard.class);
            when(mostRecentCard.getId()).thenReturn(3L);

            CharacterCard pickedCard = mock(CharacterCard.class);
            when(pickedCard.getId()).thenReturn(7L);

            // First draw (excluding the full stale history + same-day ids) finds nothing;
            // after the reset, only mostRecentCard's id remains excluded, so the second draw succeeds.
            when(characterCardService.findRandomCardExcluding(anyList()))
                    .thenReturn(null)
                    .thenReturn(pickedCard);

            // Act
            CharacterCard result = modeExclusionHistoryService.pickCardForMode(GameMode.CLASSIC, List.of(50L), mostRecentCard);

            // Assert
            assertEquals(pickedCard, result);

            ArgumentCaptor<List<Long>> excludedCaptor = ArgumentCaptor.forClass(List.class);
            verify(characterCardService, times(2)).findRandomCardExcluding(excludedCaptor.capture());
            assertEquals(Set.of(1L, 2L, 3L, 50L), new HashSet<>(excludedCaptor.getAllValues().get(0)));
            assertEquals(Set.of(3L, 50L), new HashSet<>(excludedCaptor.getAllValues().get(1)));

            verify(repo).save(history);
            assertEquals(Set.of(3L, 7L), history.getExcludedIds());
        }

        @Test
        void pickCardForMode_poolExhaustedWithNoMostRecentPick_resetsToEmptyAndRedraws() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.CLASSIC)
                    .excludedIds(new HashSet<>(Set.of(1L, 2L)))
                    .build();
            when(repo.findByGameMode(GameMode.CLASSIC)).thenReturn(Optional.of(history));

            CharacterCard pickedCard = mock(CharacterCard.class);
            when(pickedCard.getId()).thenReturn(9L);

            when(characterCardService.findRandomCardExcluding(anyList()))
                    .thenReturn(null)
                    .thenReturn(pickedCard);

            // Act
            CharacterCard result = modeExclusionHistoryService.pickCardForMode(GameMode.CLASSIC, List.of(40L), null);

            // Assert
            assertEquals(pickedCard, result);

            ArgumentCaptor<List<Long>> excludedCaptor = ArgumentCaptor.forClass(List.class);
            verify(characterCardService, times(2)).findRandomCardExcluding(excludedCaptor.capture());
            assertEquals(Set.of(40L), new HashSet<>(excludedCaptor.getAllValues().get(1)));

            assertEquals(Set.of(9L), history.getExcludedIds());
        }

        @Test
        void pickCardForMode_stillExhaustedAfterReset_throwsIllegalStateException() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.CLASSIC)
                    .excludedIds(new HashSet<>(Set.of(1L)))
                    .build();
            when(repo.findByGameMode(GameMode.CLASSIC)).thenReturn(Optional.of(history));
            when(characterCardService.findRandomCardExcluding(anyList())).thenReturn(null);

            // Act & Assert
            assertThrows(IllegalStateException.class, () ->
                    modeExclusionHistoryService.pickCardForMode(GameMode.CLASSIC, List.of(50L), null));

            verify(characterCardService, times(2)).findRandomCardExcluding(anyList());
            verify(repo, never()).save(any());
        }
    }

    @Nested
    class PickQuoteTests {

        @Test
        void pickQuote_historyExistsAndDrawSucceeds_excludesHistoryAndRecordsPick() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.QUOTES_STAGE_1)
                    .excludedIds(new HashSet<>(Set.of(11L, 22L)))
                    .build();
            when(repo.findByGameMode(GameMode.QUOTES_STAGE_1)).thenReturn(Optional.of(history));

            Quote pickedQuote = mock(Quote.class);
            when(pickedQuote.getId()).thenReturn(77L);
            when(quoteService.findRandomQuoteExcluding(anyList())).thenReturn(pickedQuote);

            // Act
            Quote result = modeExclusionHistoryService.pickQuote(null);

            // Assert
            assertEquals(pickedQuote, result);

            ArgumentCaptor<List<Long>> excludedCaptor = ArgumentCaptor.forClass(List.class);
            verify(quoteService).findRandomQuoteExcluding(excludedCaptor.capture());
            assertEquals(Set.of(11L, 22L), new HashSet<>(excludedCaptor.getValue()));

            verify(quoteService, never()).findRandomQuote();
            verify(repo).save(history);
            assertEquals(Set.of(11L, 22L, 77L), history.getExcludedIds());
        }

        @Test
        void pickQuote_noHistoryRowYet_drawsUnfilteredInsteadOfExcludingWithEmptyList() {
            // Arrange
            when(repo.findByGameMode(GameMode.QUOTES_STAGE_1)).thenReturn(Optional.empty());

            Quote pickedQuote = mock(Quote.class);
            when(pickedQuote.getId()).thenReturn(3L);
            when(quoteService.findRandomQuote()).thenReturn(pickedQuote);

            // Act
            Quote result = modeExclusionHistoryService.pickQuote(null);

            // Assert
            assertEquals(pickedQuote, result);
            verify(quoteService).findRandomQuote();
            // An empty NOT IN (...) list is invalid SQL, so the excluding query must never be called with one.
            verify(quoteService, never()).findRandomQuoteExcluding(any());
        }

        @Test
        void pickQuote_poolExhausted_resetsToMostRecentQuoteAndRedraws() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.QUOTES_STAGE_1)
                    .excludedIds(new HashSet<>(Set.of(1L, 2L, 3L)))
                    .build();
            when(repo.findByGameMode(GameMode.QUOTES_STAGE_1)).thenReturn(Optional.of(history));

            Quote mostRecentQuote = mock(Quote.class);
            when(mostRecentQuote.getId()).thenReturn(3L);

            Quote pickedQuote = mock(Quote.class);
            when(pickedQuote.getId()).thenReturn(8L);

            when(quoteService.findRandomQuoteExcluding(anyList()))
                    .thenReturn(null)
                    .thenReturn(pickedQuote);

            // Act
            Quote result = modeExclusionHistoryService.pickQuote(mostRecentQuote);

            // Assert
            assertEquals(pickedQuote, result);

            ArgumentCaptor<List<Long>> excludedCaptor = ArgumentCaptor.forClass(List.class);
            verify(quoteService, times(2)).findRandomQuoteExcluding(excludedCaptor.capture());
            assertEquals(Set.of(1L, 2L, 3L), new HashSet<>(excludedCaptor.getAllValues().get(0)));
            assertEquals(Set.of(3L), new HashSet<>(excludedCaptor.getAllValues().get(1)));

            assertEquals(Set.of(3L, 8L), history.getExcludedIds());
        }

        @Test
        void pickQuote_stillExhaustedAfterReset_throwsIllegalStateException() {
            // Arrange
            ModeExclusionHistory history = ModeExclusionHistory.builder()
                    .gameMode(GameMode.QUOTES_STAGE_1)
                    .excludedIds(new HashSet<>(Set.of(1L)))
                    .build();
            when(repo.findByGameMode(GameMode.QUOTES_STAGE_1)).thenReturn(Optional.of(history));
            when(quoteService.findRandomQuoteExcluding(anyList())).thenReturn(null);

            Quote mostRecentQuote = mock(Quote.class);
            when(mostRecentQuote.getId()).thenReturn(99L);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> modeExclusionHistoryService.pickQuote(mostRecentQuote));

            verify(repo, never()).save(any());
        }
    }
}
