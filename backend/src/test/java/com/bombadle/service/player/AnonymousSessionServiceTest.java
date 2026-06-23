package com.bombadle.service.player;

import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.ClassicGuessAttempt;
import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.AnonymousSessionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymousSessionServiceTest {

    @Mock
    private AnonymousSessionRepository repo;

    @InjectMocks
    private AnonymousSessionService anonymousSessionService;

    @Nested
    class GetAnonymousSessionOrCreateNewTests {

        @Test
        void getAnonymousSessionOrCreateNew_sessionIdIsNull_createsAndSavesNewSession() {
            // ARRANGE
            when(repo.save(any(AnonymousSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionOrCreateNew(null);

            // ASSERT
            assertNotNull(result);
            verify(repo).save(any(AnonymousSession.class));
            verify(repo, never()).findById(any(UUID.class));
        }

        @Test
        void getAnonymousSessionOrCreateNew_sessionExists_returnsExistingSessionDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            AnonymousSession existingSession = AnonymousSession.createEmptySession();
            existingSession.setId(sessionId);
            when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));
            when(repo.save(existingSession)).thenReturn(existingSession);

            // ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionOrCreateNew(sessionId);

            // ASSERT
            assertNotNull(result);
            assertEquals(sessionId, result.id());
            verify(repo).findById(sessionId);
            verify(repo).save(existingSession);
        }

        @Test
        void getAnonymousSessionOrCreateNew_sessionNotFound_createsAndSavesNewSession() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());
            when(repo.save(any(AnonymousSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionOrCreateNew(sessionId);

            // ASSERT
            assertNotNull(result);
            verify(repo).findById(sessionId);
            verify(repo).save(any(AnonymousSession.class));
        }
    }

    @Nested
    class GetAnonymousSessionReadOnlyTests {

        @Test
        void getAnonymousSessionReadOnly_sessionIdIsNull_returnsEmptySessionDto() {
            // ARRANGE & ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionReadOnly(null);

            // ASSERT
            assertNotNull(result);
            verifyNoInteractions(repo);
        }

        @Test
        void getAnonymousSessionReadOnly_sessionExists_returnsExistingSessionDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            AnonymousSession existingSession = AnonymousSession.createEmptySession();
            existingSession.setId(sessionId);
            when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));

            // ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionReadOnly(sessionId);

            // ASSERT
            assertNotNull(result);
            assertEquals(sessionId, result.id());
            verify(repo).findById(sessionId);
            verify(repo, never()).save(any());
        }

        @Test
        void getAnonymousSessionReadOnly_sessionNotFound_returnsEmptySessionDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());

            // ACT
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSessionReadOnly(sessionId);

            // ASSERT
            assertNotNull(result);
            assertNull(result.id()); // ID should be null for the freshly created empty unpersisted session
            verify(repo).findById(sessionId);
            verify(repo, never()).save(any());
        }
    }

    @Nested
    class GetGuessListTests {

        @Test
        void getGuessList_sessionIdIsNull_returnsEmptyDto() {
            // ARRANGE
            GameMode gameMode = GameMode.CLASSIC;

            // ACT
            GuessListDto result = anonymousSessionService.getGuessList(null, gameMode);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
            verifyNoInteractions(repo);
        }

        @Test
        void getGuessList_sessionExistsAndHasList_returnsDtoWithGuesses() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            GameMode gameMode = GameMode.CLASSIC;

            ClassicGuessAttempt mockAttempt = mock(ClassicGuessAttempt.class);

            AnonymousGuessList guessList = AnonymousGuessList.builder()
                    .gameMode(gameMode)
                    .guesses(List.of(mockAttempt))
                    .build();

            AnonymousSession session = AnonymousSession.builder()
                    .guessLists(List.of(guessList))
                    .build();

            when(repo.findById(sessionId)).thenReturn(Optional.of(session));

            // ACT
            GuessListDto result = anonymousSessionService.getGuessList(sessionId, gameMode);

            // ASSERT
            assertNotNull(result);
            assertFalse(result.guessList().isEmpty());
            assertEquals(1, result.guessList().size());
            verify(repo).findById(sessionId);
        }

        @Test
        void getGuessList_sessionExistsButNoListForMode_returnsEmptyDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            GameMode gameMode = GameMode.QUOTES_STAGE_1;

            AnonymousGuessList guessList = AnonymousGuessList.builder()
                    .gameMode(GameMode.CLASSIC)
                    .guesses(List.of(mock(ClassicGuessAttempt.class)))
                    .build();

            AnonymousSession session = AnonymousSession.builder()
                    .guessLists(List.of(guessList))
                    .build();

            when(repo.findById(sessionId)).thenReturn(Optional.of(session));

            // ACT
            GuessListDto result = anonymousSessionService.getGuessList(sessionId, gameMode);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
            verify(repo).findById(sessionId);
        }

        @Test
        void getGuessList_sessionNotFound_returnsEmptyDto() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            GameMode gameMode = GameMode.CLASSIC;
            when(repo.findById(sessionId)).thenReturn(Optional.empty());

            // ACT
            GuessListDto result = anonymousSessionService.getGuessList(sessionId, gameMode);

            // ASSERT
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
            verify(repo).findById(sessionId);
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_validSession_callsRepositorySave() {
            // ARRANGE
            AnonymousSession session = AnonymousSession.createEmptySession();
            when(repo.save(session)).thenReturn(session);

            // ACT
            AnonymousSession result = anonymousSessionService.save(session);

            // ASSERT
            assertEquals(session, result);
            verify(repo).save(session);
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_sessionExists_returnsOptionalWithSession() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = AnonymousSession.createEmptySession();
            when(repo.findById(sessionId)).thenReturn(Optional.of(session));

            // ACT
            Optional<AnonymousSession> result = anonymousSessionService.findById(sessionId);

            // ASSERT
            assertTrue(result.isPresent());
            assertEquals(session, result.get());
        }

        @Test
        void findById_sessionDoesNotExist_returnsEmptyOptional() {
            // ARRANGE
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());

            // ACT
            Optional<AnonymousSession> result = anonymousSessionService.findById(sessionId);

            // ASSERT
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_validSession_callsRepositoryDelete() {
            // ARRANGE
            AnonymousSession session = AnonymousSession.createEmptySession();

            // ACT
            anonymousSessionService.delete(session);

            // ASSERT
            verify(repo).delete(session);
        }
    }

    @Nested
    class TruncateTableTests {

        @Test
        void truncateTable_called_callsRepositoryTruncateTable() {
            // ARRANGE

            // ACT
            anonymousSessionService.truncateTable();

            // ASSERT
            verify(repo).truncateTable();
        }
    }
}