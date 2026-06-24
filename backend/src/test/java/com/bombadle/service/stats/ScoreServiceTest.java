package com.bombadle.service.stats;

import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {

    @Mock
    private ScoreRepository repo;

    @InjectMocks
    private ScoreService scoreService;

    @Nested
    class SaveScoreTests {

        @Test
        void saveScore_validScore_returnsSavedScore() {
            // Arrange
            Score score = mock(Score.class);
            when(repo.save(score)).thenReturn(score);

            // Act
            Score savedScore = scoreService.saveScore(score);

            // Assert
            assertEquals(score, savedScore);
            verify(repo).save(score);
        }

        @Test
        void saveScore_scoreIsNull_throwsIllegalArgumentException() {
            // Act
            // Assert
            assertThrows(IllegalArgumentException.class, () -> scoreService.saveScore(null));
            verifyNoInteractions(repo);
        }
    }

    @Nested
    class GetAllScoresTests {

        @Test
        void getAllScores_called_returnsListOfScores() {
            // Arrange
            List<Score> expectedList = List.of(mock(Score.class));
            when(repo.findAll()).thenReturn(expectedList);

            // Act
            List<Score> result = scoreService.getAllScores();

            // Assert
            assertEquals(expectedList, result);
            verify(repo).findAll();
        }

        @Test
        void getAllScores_withPageable_returnsPageOfScores() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Score> expectedPage = new PageImpl<>(List.of(mock(Score.class)));
            when(repo.findAll(pageable)).thenReturn(expectedPage);

            // Act
            Page<Score> result = scoreService.getAllScores(pageable);

            // Assert
            assertEquals(expectedPage, result);
            verify(repo).findAll(pageable);
        }
    }

    @Nested
    class FindScoreTests {

        @Test
        void findByPlayerId_scoreExists_returnsOptionalWithScore() {
            // Arrange
            Long playerId = 1L;
            Score score = mock(Score.class);
            when(repo.findByPlayerId(playerId)).thenReturn(Optional.of(score));

            // Act
            Optional<Score> result = scoreService.findByPlayerId(playerId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(score, result.get());
            verify(repo).findByPlayerId(playerId);
        }

        @Test
        void findScoreByPlayerEmail_scoreExists_returnsOptionalWithScore() {
            // Arrange
            String email = "test@test.test";
            Score score = mock(Score.class);
            when(repo.findByPlayerEmail(email)).thenReturn(Optional.of(score));

            // Act
            Optional<Score> result = scoreService.findScoreByPlayerEmail(email);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(score, result.get());
            verify(repo).findByPlayerEmail(email);
        }

        @Test
        void getScoreById_scoreExists_returnsOptionalWithScore() {
            // Arrange
            Long scoreId = 1L;
            Score score = mock(Score.class);
            when(repo.findById(scoreId)).thenReturn(Optional.of(score));

            // Act
            Optional<Score> result = scoreService.getScoreById(scoreId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(score, result.get());
            verify(repo).findById(scoreId);
        }

        @Test
        void findLatestScoreTimestamp_timestampExists_returnsOptionalWithTimestamp() {
            // Arrange
            Instant latestTime = Instant.now();
            when(repo.findLatestScoreTimestamp()).thenReturn(Optional.of(latestTime));

            // Act
            Optional<Instant> result = scoreService.findLatestScoreTimestamp();

            // Assert
            assertTrue(result.isPresent());
            assertEquals(latestTime, result.get());
            verify(repo).findLatestScoreTimestamp();
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_validScore_callsRepositorySave() {
            // Arrange
            Score score = mock(Score.class);
            when(repo.save(score)).thenReturn(score);

            // Act
            Score result = scoreService.save(score);

            // Assert
            assertEquals(score, result);
            verify(repo).save(score);
        }
    }

    @Nested
    class DeletionTests {

        @Test
        void manualDelete_validScore_callsRepositoryDelete() {
            // Arrange
            Score score = mock(Score.class);

            // Act
            scoreService.manualDelete(score);

            // Assert
            verify(repo).delete(score);
        }

        @Test
        void deleteScoreById_validId_callsRepositoryDeleteById() {
            // Arrange
            Long scoreId = 1L;

            // Act
            scoreService.deleteScoreById(scoreId);

            // Assert
            verify(repo).deleteById(scoreId);
        }

        @Test
        void deleteAllInBatch_called_callsRepositoryDeleteAllInBatch() {
            // Act
            scoreService.deleteAllInBatch();

            // Assert
            verify(repo).deleteAllInBatch();
        }

        @Test
        void deleteAllByPlayerId_validId_callsRepositoryDeleteByPlayerId() {
            // Arrange
            Long playerId = 1L;

            // Act
            scoreService.deleteAllByPlayerId(playerId);

            // Assert
            verify(repo).deleteByPlayerId(playerId);
        }
    }

    @Nested
    class ExistsByIdTests {

        @Test
        void existsById_idExists_returnsTrue() {
            // Arrange
            Long scoreId = 1L;
            when(repo.existsById(scoreId)).thenReturn(true);

            // Act
            boolean result = scoreService.existsById(scoreId);

            // Assert
            assertTrue(result);
            verify(repo).existsById(scoreId);
        }
    }
}