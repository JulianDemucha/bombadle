package com.bombadle.service.auth;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.*;
import com.bombadle.service.game.AnonymousGuessListService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.ScoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnonymousMergeServiceTest {

    @InjectMocks
    private AnonymousMergeService anonymousMergeService;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @Mock
    private ScoreService scoreService;

    @Mock
    private PlayerService playerService;

    @Mock
    private GuessListService guessListService;

    @Mock
    private AnonymousGuessListService anonymousGuessListService;

    private final UUID dummyUUID = UUID.randomUUID();

    @Nested
    class HandleAnonymousSessionMergeTests {

        @Test
        void handleAnonymousSessionMerge_anonymousSessionIdIsNull_doesNothing() {
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().build(), null, null);
            verifyNoInteractions(anonymousSessionService, scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_triggerMergeIdIsNull_doesNothing() {
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().build(), dummyUUID, null);
            verifyNoInteractions(anonymousSessionService, scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_triggerMergeIdIsFalse_doesNothing() {
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().build(), dummyUUID, false);
            verifyNoInteractions(anonymousSessionService, scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_playerHasGuessedToday_doesNothing() {
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().hasGuessedToday(true).build(), dummyUUID, true);
            verifyNoInteractions(anonymousSessionService, scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_sessionDoesntExist_doesNothing() {
            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.empty());
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().hasGuessedToday(false).build(), dummyUUID, true);
            verifyNoInteractions(scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_anonymousHasNotGuessedToday_doesNothing() {
            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.of(AnonymousSession.builder().hasGuessedToday(false).build()));
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().hasGuessedToday(false).build(), dummyUUID, true);
            verifyNoInteractions(scoreService, playerService, guessListService, anonymousGuessListService);
        }

        @Test
        void handleAnonymousSessionMerge_everythingIsValid_mergeSuccess() {
            // Arrange
            Instant scoreTimeStamp = Instant.now().minusSeconds(60);
            Player player = Player.builder().hasGuessedToday(false).build();

            AnonymousGuessList anonGuessList = AnonymousGuessList.builder()
                    .guesses(List.of(GuessAttempt.builder().build(), GuessAttempt.builder().build())) // size 2
                    .build();

            AnonymousSession session = AnonymousSession.builder()
                    .scoreTimestamp(scoreTimeStamp)
                    .hasGuessedToday(true)
                    .guessList(anonGuessList)
                    .build();

            Score generatedScore = Score.builder().numberOfTries(2).scoreTimestamp(scoreTimeStamp).player(player).build();

            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.of(session));
            when(scoreService.registerScoreWithTimestamp(player, 2, scoreTimeStamp)).thenReturn(generatedScore);

            // Act
            anonymousMergeService.handleAnonymousSessionMerge(player, dummyUUID, true);

            // Assert
            verify(scoreService).registerScoreWithTimestamp(player, 2, scoreTimeStamp);

            ArgumentCaptor<GuessList> guessListCaptor = ArgumentCaptor.forClass(GuessList.class);
            verify(guessListService).manualSave(guessListCaptor.capture());

            GuessList savedGuessList = guessListCaptor.getValue();
            assertThat(savedGuessList.getGuesses()).hasSize(2);
            assertThat(savedGuessList.getPlayer()).isEqualTo(player);

            verify(playerService).registerScore(player, generatedScore);

            verify(anonymousGuessListService).delete(anonGuessList);
            verify(anonymousSessionService).delete(session);
        }

        @Test
        void handleAnonymousSessionMerge_guessListIsNull_doesNothing() {
            // Arrange
            AnonymousSession sessionWithoutGuessList = AnonymousSession.builder()
                    .hasGuessedToday(true)
                    .guessList(null)
                    .build();

            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.of(sessionWithoutGuessList));

            // Act
            anonymousMergeService.handleAnonymousSessionMerge(
                    Player.builder().hasGuessedToday(false).build(),
                    dummyUUID,
                    true
            );

            // Assert
            verifyNoInteractions(scoreService, guessListService, playerService, anonymousGuessListService);
        }
    }
}