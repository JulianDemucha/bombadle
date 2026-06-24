package com.bombadle.service.auth;

import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.*;
import com.bombadle.enums.GameMode;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.game.ScoreRegistrationService;
import com.bombadle.service.player.AnonymousSessionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private GuessListService guessListService;

    @Mock
    private ScoreRegistrationService scoreRegistrationService;

    @Nested
    class HandleAnonymousSessionMergeTests {

        @Test
        void handleAnonymousSessionMerge_anonymousSessionIdIsNull_doesNothing() {
            // ARRANGE
            Player player = Player.builder().build();

            // ACT
            anonymousMergeService.handleAnonymousSessionMerge(player, null, true);

            // ASSERT
            verifyNoInteractions(anonymousSessionService, guessListService, scoreRegistrationService);
        }

        @Test
        void handleAnonymousSessionMerge_triggerMergeIdIsFalse_doesNothing() {
            // ARRANGE
            Player player = Player.builder().build();
            UUID dummyUUID = UUID.randomUUID();

            // ACT
            anonymousMergeService.handleAnonymousSessionMerge(player, dummyUUID, false);

            // ASSERT
            verifyNoInteractions(anonymousSessionService, guessListService, scoreRegistrationService);
        }

        @Test
        void handleAnonymousSessionMerge_sessionDoesntExist_doesNothing() {
            // ARRANGE
            Player player = Player.builder().build();
            UUID dummyUUID = UUID.randomUUID();
            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.empty());

            // ACT
            anonymousMergeService.handleAnonymousSessionMerge(player, dummyUUID, true);

            // ASSERT
            verifyNoInteractions(guessListService, scoreRegistrationService);
        }

        @Test
        void handleAnonymousSessionMerge_everythingIsValidAndGuessCorrect_mergeSuccessAndScoreRegistered() {
            // ARRANGE
            UUID dummyUUID = UUID.randomUUID();
            Player player = mock(Player.class);
            when(player.hasGuessedToday(GameMode.CLASSIC)).thenReturn(false);

            ClassicGuessAttempt attempt1 = mock(ClassicGuessAttempt.class);
            ClassicGuessAttempt attempt2 = mock(ClassicGuessAttempt.class);
            when(attempt2.isCorrect()).thenReturn(true);

            AnonymousGuessList anonGuessList = AnonymousGuessList.builder()
                    .gameMode(GameMode.CLASSIC)
                    .guesses(List.of(attempt1, attempt2))
                    .build();

            Map<GameMode, Instant> timestamps = Map.of(GameMode.CLASSIC, Instant.now());
            AnonymousSession session = AnonymousSession.builder()
                    .guessLists(List.of(anonGuessList))
                    .scoreTimestamps(timestamps)
                    .build();

            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.of(session));

            // ACT
            anonymousMergeService.handleAnonymousSessionMerge(player, dummyUUID, true);

            // ASSERT
            ArgumentCaptor<GuessList> guessListCaptor = ArgumentCaptor.forClass(GuessList.class);
            verify(guessListService).save(guessListCaptor.capture());

            GuessList savedGuessList = guessListCaptor.getValue();
            assertThat(savedGuessList.getGuesses()).hasSize(2);
            assertThat(savedGuessList.getGameMode()).isEqualTo(GameMode.CLASSIC);

            verify(scoreRegistrationService).registerPlayerWinWithTimestamp(
                    player.getId(),
                    2,
                    GameMode.CLASSIC,
                    session.getScoreTimestamps().get(GameMode.CLASSIC)
            );
            verify(anonymousSessionService).delete(session);
        }

        @Test
        void handleAnonymousSessionMerge_guessListsIsNull_deletesSessionAndDoesNothingElse() {
            // ARRANGE
            UUID dummyUUID = UUID.randomUUID();
            AnonymousSession sessionWithoutGuessList = AnonymousSession.builder()
                    .guessLists(null)
                    .build();

            when(anonymousSessionService.findById(dummyUUID)).thenReturn(Optional.of(sessionWithoutGuessList));

            // ACT
            anonymousMergeService.handleAnonymousSessionMerge(Player.builder().build(), dummyUUID, true);

            // ASSERT
            verify(anonymousSessionService).delete(sessionWithoutGuessList);
            verifyNoInteractions(guessListService, scoreRegistrationService);
        }
    }
}