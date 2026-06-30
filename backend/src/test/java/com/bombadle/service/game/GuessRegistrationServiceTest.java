package com.bombadle.service.game;

import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuessRegistrationServiceTest {

    @Mock
    private GuessListService guessListService;

    @Mock
    private ScoreRegistrationService scoreRegistrationService;

    @InjectMocks
    private GuessRegistrationService guessRegistrationService;

    @Nested
    class RegisterGuessTests {

        @Test
        void registerGuess_incorrectGuess_addsGuessAndSavesWithoutScore() {
            // ARRANGE
            Player player = mock(Player.class);
            ClassicGuessAttempt attempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;
            GuessList guessList = GuessList.builder().gameMode(mode).build();

            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, mode)).thenReturn(guessList);
            when(attempt.isCorrect()).thenReturn(false);

            // ACT
            guessRegistrationService.registerGuess(player, attempt, mode);

            // ASSERT
            assertTrue(guessList.getGuesses().contains(attempt));
            verify(guessListService).save(guessList);
            verifyNoInteractions(scoreRegistrationService);
        }

        @Test
        void registerGuess_correctGuess_addsGuessRegistersScoreAndSaves() {
            // ARRANGE
            Player player = mock(Player.class);
            ClassicGuessAttempt attempt = mock(ClassicGuessAttempt.class);
            GameMode mode = GameMode.CLASSIC;
            GuessList guessList = GuessList.builder().gameMode(mode).build();

            when(guessListService.findByPlayerAndGameModeOrElseCreateNew(player, mode)).thenReturn(guessList);
            when(attempt.isCorrect()).thenReturn(true);

            // ACT
            guessRegistrationService.registerGuess(player, attempt, mode);

            // ASSERT
            assertTrue(guessList.getGuesses().contains(attempt));
            verify(scoreRegistrationService).registerPlayerWin(player.getId(), 1, mode);
            verify(guessListService).save(guessList);
        }
    }
}
