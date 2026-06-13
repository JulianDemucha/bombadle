package com.bombadle.service.game;

import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.GuessListRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuessListServiceTest {

    @Mock
    private GuessListRepository guessListRepository;

    @InjectMocks
    private GuessListService guessListService;

    @Nested
    class FindByPlayerIdTests {

        @Test
        void findByPlayerId_listFound_returnsGuessListOptional() {
            // ARRANGE
            long playerId = 1L;
            GuessList guessList = mock(GuessList.class);
            when(guessListRepository.findById(playerId)).thenReturn(Optional.of(guessList));

            // ACT
            Optional<GuessList> result = guessListService.findByPlayerId(playerId);

            // ASSERT
            assertTrue(result.isPresent());
            assertEquals(guessList, result.get());
        }

        @Test
        void findByPlayerId_listNotFound_returnsEmptyOptional() {
            // ARRANGE
            long playerId = 1L;
            when(guessListRepository.findById(playerId)).thenReturn(Optional.empty());

            // ACT
            Optional<GuessList> result = guessListService.findByPlayerId(playerId);

            // ASSERT
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetGuessListByPlayerIdTests {

        @Test
        void getGuessListByPlayerId_listExists_returnsDtoWithGuesses() {
            // ARRANGE
            long playerId = 1L;
            GameMode mode = GameMode.CLASSIC;
            GuessList guessList = mock(GuessList.class);
            ClassicGuessAttempt attempt = mock(ClassicGuessAttempt.class);
            List<GuessAttempt> guesses = List.of(attempt);

            when(guessListRepository.findByPlayerIdAndGameMode(playerId, mode)).thenReturn(Optional.of(guessList));
            when(guessList.getGuesses()).thenReturn(guesses);

            // ACT
            GuessListDto result = guessListService.getGuessListByPlayerId(playerId, mode);

            // ASSERT
            assertNotNull(result);
            assertEquals(1, result.guessList().size());
            assertEquals(attempt, result.guessList().getFirst());
        }

        @Test
        void getGuessListByPlayerId_listDoesNotExist_returnsDtoWithEmptyList() {
            // ARRANGE
            long playerId = 1L;
            GameMode mode = GameMode.CLASSIC;
            when(guessListRepository.findByPlayerIdAndGameMode(playerId, mode)).thenReturn(Optional.empty());

            // ACT
            GuessListDto result = guessListService.getGuessListByPlayerId(playerId, mode);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
        }
    }

    @Nested
    class FindByPlayerAndGameModeOrElseCreateNewTests {

        @Test
        void findByPlayerAndGameModeOrElseCreateNew_listExists_returnsExistingList() {
            // ARRANGE
            Player player = mock(Player.class);
            GameMode mode = GameMode.CLASSIC;
            GuessList guessList = mock(GuessList.class);
            when(player.getId()).thenReturn(1L);
            when(guessListRepository.findByPlayerIdAndGameMode(1L, mode)).thenReturn(Optional.of(guessList));

            // ACT
            GuessList result = guessListService.findByPlayerAndGameModeOrElseCreateNew(player, mode);

            // ASSERT
            assertEquals(guessList, result);
        }

        @Test
        void findByPlayerAndGameModeOrElseCreateNew_listDoesNotExist_returnsNewList() {
            // ARRANGE
            Player player = mock(Player.class);
            GameMode mode = GameMode.CLASSIC;
            when(player.getId()).thenReturn(1L);
            when(guessListRepository.findByPlayerIdAndGameMode(1L, mode)).thenReturn(Optional.empty());

            // ACT
            GuessList result = guessListService.findByPlayerAndGameModeOrElseCreateNew(player, mode);

            // ASSERT
            assertNotNull(result);
            assertEquals(player, result.getPlayer());
            assertEquals(mode, result.getGameMode());
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_validList_callsRepositorySave() {
            // ARRANGE
            GuessList guessList = mock(GuessList.class);

            // ACT
            guessListService.save(guessList);

            // ASSERT
            verify(guessListRepository).save(guessList);
        }
    }

    @Nested
    class DeletionTests {

        @Test
        void manualDelete_validList_callsRepositoryDelete() {
            // ARRANGE
            GuessList guessList = mock(GuessList.class);

            // ACT
            guessListService.manualDelete(guessList);

            // ASSERT
            verify(guessListRepository).delete(guessList);
        }

        @Test
        void truncateTable_called_callsRepositoryTruncate() {
            // ACT
            guessListService.truncateTable();

            // ASSERT
            verify(guessListRepository).truncateTable();
        }

        @Test
        void deleteAllByPlayerId_called_callsRepositoryDeleteAll() {
            // ARRANGE
            long playerId = 1L;

            // ACT
            guessListService.deleteAllByPlayerId(playerId);

            // ASSERT
            verify(guessListRepository).deleteByPlayerId(playerId);
        }
    }
}
