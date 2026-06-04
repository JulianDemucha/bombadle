package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.repository.GuessListRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
            // Arrange
            long playerId = 1L;
            GuessList guessList = mock(GuessList.class);
            when(guessListRepository.findById(playerId)).thenReturn(Optional.of(guessList));

            // Act
            Optional<GuessList> result = guessListService.findByPlayerId(playerId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(guessList, result.get());
        }

        @Test
        void findByPlayerId_listNotFound_returnsEmptyOptional() {
            // Arrange
            long playerId = 1L;
            when(guessListRepository.findById(playerId)).thenReturn(Optional.empty());

            // Act
            Optional<GuessList> result = guessListService.findByPlayerId(playerId);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetGuessListByPlayerIdTests {

        @Test
        void getGuessListByPlayerId_listExists_returnsDtoWithGuesses() {
            // Arrange
            long playerId = 1L;
            GuessList guessList = mock(GuessList.class);
            GuessAttempt attempt = mock(GuessAttempt.class);
            List<GuessAttempt> guesses = List.of(attempt);

            when(guessListRepository.findByPlayerId(playerId)).thenReturn(Optional.of(guessList));
            when(guessList.getGuesses()).thenReturn(guesses);

            // Act
            GuessListDto result = guessListService.getGuessListByPlayerId(playerId);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.guessList().size());
            assertEquals(attempt, result.guessList().getFirst());
        }

        @Test
        void getGuessListByPlayerId_listDoesNotExist_returnsDtoWithEmptyList() {
            // Arrange
            long playerId = 1L;
            when(guessListRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());

            // Act
            GuessListDto result = guessListService.getGuessListByPlayerId(playerId);

            // Assert
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
        }
    }

    @Nested
    class FindByPlayerOrElseCreateNewTests {

        @Test
        void findByPlayerOrElseCreateNew_listExists_returnsExistingList() {
            // Arrange
            Player player = mock(Player.class);
            GuessList guessList = mock(GuessList.class);
            when(player.getId()).thenReturn(1L);
            when(guessListRepository.findByPlayerId(1L)).thenReturn(Optional.of(guessList));

            // Act
            GuessList result = guessListService.findByPlayerOrElseCreateNew(player);

            // Assert
            assertEquals(guessList, result);
        }

        @Test
        void findByPlayerOrElseCreateNew_listDoesNotExist_returnsNewList() {
            // Arrange
            Player player = mock(Player.class);
            when(player.getId()).thenReturn(1L);
            when(guessListRepository.findByPlayerId(1L)).thenReturn(Optional.empty());

            // Act
            GuessList result = guessListService.findByPlayerOrElseCreateNew(player);

            // Assert
            assertNotNull(result);
            assertEquals(player, result.getPlayer());
        }
    }

    @Nested
    class RegisterGuessWithPlayerTests {

        @Test
        void registerGuess_withPlayer_addsGuessAndSaves() {
            // Arrange
            Player player = mock(Player.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);
            GuessList guessList = mock(GuessList.class);
            List<GuessAttempt> guesses = new ArrayList<>();

            when(player.getId()).thenReturn(1L);
            when(guessListRepository.findByPlayerId(1L)).thenReturn(Optional.of(guessList));
            when(guessList.getGuesses()).thenReturn(guesses);

            // Act
            guessListService.registerGuess(player, guessAttempt);

            // Assert
            assertEquals(1, guesses.size());
            assertEquals(guessAttempt, guesses.getFirst());
            verify(guessListRepository).save(guessList);
        }
    }

    @Nested
    class ManualSaveTests {

        @Test
        void manualSave_validList_callsRepositorySave() {
            // Arrange
            GuessList guessList = mock(GuessList.class);

            // Act
            guessListService.manualSave(guessList);

            // Assert
            verify(guessListRepository).save(guessList);
        }
    }

    @Nested
    class RegisterGuessWithListTests {

        @Test
        void registerGuess_withGuessList_addsGuessAndSaves() {
            // Arrange
            GuessList guessList = mock(GuessList.class);
            GuessAttempt guessAttempt = mock(GuessAttempt.class);
            List<GuessAttempt> guesses = new ArrayList<>();

            when(guessList.getGuesses()).thenReturn(guesses);

            // Act
            guessListService.registerGuess(guessList, guessAttempt);

            // Assert
            assertEquals(1, guesses.size());
            assertEquals(guessAttempt, guesses.getFirst());
            verify(guessListRepository).save(guessList);
        }
    }

    @Nested
    class TruncateTableTests {

        @Test
        void truncateTable_called_callsRepositoryTruncate() {
            // Act
            guessListService.truncateTable();

            // Assert
            verify(guessListRepository).truncateTable();
        }
    }
}