package com.bombadle.service.game;

import com.bombadle.dto.CardField;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.MatchType;
import com.bombadle.repository.AnonymousGuessListRepository;
import com.bombadle.service.player.AnonymousSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymousGuessListServiceTest {

    @InjectMocks
    private AnonymousGuessListService service;

    @Mock
    private AnonymousGuessListRepository repo;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    private AnonymousSession session;
    private AnonymousGuessList guessList;
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guessList = AnonymousGuessList.builder()
                .guesses(new ArrayList<>())
                .build();

        session = AnonymousSession.builder()
                .id(sessionId)
                .hasGuessedToday(false)
                .guessList(guessList)
                .build();
    }

    @Nested
    class RegisterGuessAndGetSessionIdTests {

        @Test
        void registerGuessAndGetSessionId_incorrectGuess_addsGuessAndReturnsId() {
            // Arrange
            GuessAttempt attempt = GuessAttempt.builder().name(new CardField<>("sigma_card", MatchType.NOT_MATCH)).build();
            when(repo.save(guessList)).thenReturn(guessList);
            when(anonymousSessionService.save(session)).thenReturn(session);

            // Act
            UUID resultId = service.registerGuessAndGetSessionId(session, attempt);

            // Assert
            assertEquals(sessionId, resultId);
            assertTrue(guessList.getGuesses().contains(attempt));
            assertFalse(session.isHasGuessedToday());
            assertNull(session.getScoreTimestamp());

            verify(repo).save(guessList);
            verify(anonymousSessionService).save(session);
        }

        @Test
        void registerGuessAndGetSessionId_correctGuess_addsGuessSetsTimestampAndReturnsId() {
            // Arrange
            GuessAttempt attempt = GuessAttempt.builder().name(new CardField<>("sigma_card", MatchType.MATCH)).build();
            when(repo.save(guessList)).thenReturn(guessList);
            when(anonymousSessionService.save(session)).thenReturn(session);

            // Act
            UUID resultId = service.registerGuessAndGetSessionId(session, attempt);

            // Assert
            assertEquals(sessionId, resultId);
            assertTrue(guessList.getGuesses().contains(attempt));
            assertTrue(session.isHasGuessedToday());
            assertNotNull(session.getScoreTimestamp());

            verify(repo).save(guessList);
            verify(anonymousSessionService).save(session);
        }
    }

    @Nested
    class RegisterGuessTests {

        @Test
        void registerGuess_incorrectGuess_addsGuessWithoutSettingTimestamp() {
            // Arrange
            GuessAttempt attempt = GuessAttempt.builder().name(new CardField<>("sigma_card", MatchType.NOT_MATCH)).build();
            when(repo.save(guessList)).thenReturn(guessList);

            // Act
            service.registerGuess(session, attempt);

            // Assert
            assertTrue(guessList.getGuesses().contains(attempt));
            assertFalse(session.isHasGuessedToday());
            assertNull(session.getScoreTimestamp());

            verify(repo).save(guessList);
            verify(anonymousSessionService).save(session);
        }

        @Test
        void registerGuess_correctGuess_addsGuessAndSetsTimestamp() {
            // Arrange
            GuessAttempt attempt = GuessAttempt.builder().name(new CardField<>("sigma_card", MatchType.MATCH)).build();
            when(repo.save(guessList)).thenReturn(guessList);

            // Act
            service.registerGuess(session, attempt);

            // Assert
            assertTrue(guessList.getGuesses().contains(attempt));
            assertTrue(session.isHasGuessedToday());
            assertNotNull(session.getScoreTimestamp());

            verify(repo).save(guessList);
            verify(anonymousSessionService).save(session);
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_callsRepositoryDelete() {
            // Act
            service.delete(guessList);

            // Assert
            verify(repo).delete(guessList);
        }
    }

    @Nested
    class TruncateTableTests {

        @Test
        void truncateTable_callsRepositoryTruncate() {
            // Act
            service.truncateTable();

            // Assert
            verify(repo).truncateTable();
        }
    }
}