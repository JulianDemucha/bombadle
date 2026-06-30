package com.bombadle.service.game;

import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.player.AnonymousSessionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymousGuessRegistrationServiceTest {

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AnonymousGuessRegistrationService anonymousGuessRegistrationService;

    @Nested
    class RegisterGuessAndGetSessionIdTests {

        @Test
        void registerGuessAndGetSessionId_incorrectGuess_addsGuessAndSavesWithoutTimestamp() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);
            AnonymousGuessList guessList = mock(AnonymousGuessList.class);
            ClassicGuessAttempt attempt = mock(ClassicGuessAttempt.class);
            GameMode gameMode = GameMode.CLASSIC;

            when(session.getGuessListForMode(gameMode)).thenReturn(Optional.of(guessList));
            when(attempt.isCorrect()).thenReturn(false);
            when(anonymousSessionService.save(session)).thenReturn(session);
            when(session.getId()).thenReturn(sessionId);

            // ACT
            UUID result = anonymousGuessRegistrationService.registerGuessAndGetSessionId(session, attempt, gameMode);

            // ASSERT
            assertEquals(sessionId, result);
            verify(guessList).addGuess(attempt);
            verify(session, never()).markModeAsCompleted(any());
            verify(session, never()).addScoreTimestamp(any(), any());
            verify(session).setLastActiveAt(any(Instant.class));
            verify(anonymousSessionService).save(session);
            verify(cacheService, never()).evictCacheEntry(anyString(), any());
        }

        @Test
        void registerGuessAndGetSessionId_correctGuess_addsGuessMarksCompletedAndSavesWithTimestamp() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = mock(AnonymousSession.class);
            AnonymousGuessList guessList = mock(AnonymousGuessList.class);
            ClassicGuessAttempt attempt = mock(ClassicGuessAttempt.class);
            GameMode gameMode = GameMode.QUOTES_STAGE_1;

            when(session.getGuessListForMode(gameMode)).thenReturn(Optional.of(guessList));
            when(attempt.isCorrect()).thenReturn(true);
            when(anonymousSessionService.save(session)).thenReturn(session);
            when(session.getId()).thenReturn(sessionId);

            // ACT
            UUID result = anonymousGuessRegistrationService.registerGuessAndGetSessionId(session, attempt, gameMode);

            // ASSERT
            assertEquals(sessionId, result);
            verify(guessList).addGuess(attempt);
            verify(session).markModeAsCompleted(gameMode);
            verify(session).addScoreTimestamp(eq(gameMode), any(Instant.class));
            verify(session).setLastActiveAt(any(Instant.class));
            verify(anonymousSessionService).save(session);
            verify(cacheService).evictCacheEntry("today-solvers", gameMode.name());
        }
    }
}