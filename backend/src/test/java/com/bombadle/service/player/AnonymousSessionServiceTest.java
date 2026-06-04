package com.bombadle.service.player;

import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.repository.AnonymousSessionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    class GetAnonymousSessionTests {

        @Test
        void getAnonymousSession_sessionIdIsNull_createsAndSavesNewSession() {
            // Arrange
            when(repo.save(any(AnonymousSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSession(null);

            // Assert
            assertNotNull(result);
            verify(repo).save(any(AnonymousSession.class));
            verify(repo, never()).findById(any(UUID.class));
        }

        @Test
        void getAnonymousSession_sessionExists_returnsExistingSessionDto() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            AnonymousSession existingSession = new AnonymousSession(new AnonymousGuessList());
            when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));
            when(repo.save(existingSession)).thenReturn(existingSession);

            // Act
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSession(sessionId);

            // Assert
            assertNotNull(result);
            verify(repo).findById(sessionId);
            verify(repo).save(existingSession);
        }

        @Test
        void getAnonymousSession_sessionNotFound_createsAndSavesNewSession() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());
            when(repo.save(any(AnonymousSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AnonymousSessionDto result = anonymousSessionService.getAnonymousSession(sessionId);

            // Assert
            assertNotNull(result);
            verify(repo).findById(sessionId);
            verify(repo).save(any(AnonymousSession.class));
        }
    }

    @Nested
    class GetGuessListTests {

        @Test
        void getGuessList_sessionIdIsNull_returnsEmptyDto() {
            // Arrange
            // No setup needed

            // Act
            GuessListDto result = anonymousSessionService.getGuessList(null);

            // Assert
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
            verifyNoInteractions(repo);
        }

        @Test
        void getGuessList_sessionExists_returnsDtoWithGuesses() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = new AnonymousSession(new AnonymousGuessList());
            when(repo.findById(sessionId)).thenReturn(Optional.of(session));

            // Act
            GuessListDto result = anonymousSessionService.getGuessList(sessionId);

            // Assert
            assertNotNull(result);
            verify(repo).findById(sessionId);
        }

        @Test
        void getGuessList_sessionNotFound_returnsEmptyDto() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());

            // Act
            GuessListDto result = anonymousSessionService.getGuessList(sessionId);

            // Assert
            assertNotNull(result);
            assertTrue(result.guessList().isEmpty());
            verify(repo).findById(sessionId);
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_validSession_callsRepositorySave() {
            // Arrange
            AnonymousSession session = new AnonymousSession(new AnonymousGuessList());
            when(repo.save(session)).thenReturn(session);

            // Act
            AnonymousSession result = anonymousSessionService.save(session);

            // Assert
            assertEquals(session, result);
            verify(repo).save(session);
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_sessionExists_returnsOptionalWithSession() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            AnonymousSession session = new AnonymousSession(new AnonymousGuessList());
            when(repo.findById(sessionId)).thenReturn(Optional.of(session));

            // Act
            Optional<AnonymousSession> result = anonymousSessionService.findById(sessionId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(session, result.get());
        }

        @Test
        void findById_sessionDoesNotExist_returnsEmptyOptional() {
            // Arrange
            UUID sessionId = UUID.randomUUID();
            when(repo.findById(sessionId)).thenReturn(Optional.empty());

            // Act
            Optional<AnonymousSession> result = anonymousSessionService.findById(sessionId);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_validSession_callsRepositoryDelete() {
            // Arrange
            AnonymousSession session = new AnonymousSession(new AnonymousGuessList());

            // Act
            anonymousSessionService.delete(session);

            // Assert
            verify(repo).delete(session);
        }
    }

    @Nested
    class TruncateTableTests {

        @Test
        void truncateTable_called_callsRepositoryTruncateTable() {
            // Arrange
            // No setup needed

            // Act
            anonymousSessionService.truncateTable();

            // Assert
            verify(repo).truncateTable();
        }
    }
}