package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ScoreService scoreService;

    @Captor
    private ArgumentCaptor<Score> scoreCaptor;

    private Player getExamplePlayer() {
        return Player.builder()
                .id(1L)
                .login("test")
                .passwordHash("test")
                .email("test@test.test")
                .authProvider(PlayerAuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .hasGuessedToday(true)
                .totalSuccessfulGuesses(1)
                .build();
    }

    @Nested
    class SaveScoreTests {

        @Test
        void saveScore_validScore_addsScoreSuccessfully() {
            // Arrange
            Player player = getExamplePlayer();
            Score score = Score.builder()
                    .player(player)
                    .scoreTimestamp(Instant.now())
                    .numberOfTries(5)
                    .build();
            score.setId(1L);

            when(repo.save(score)).thenReturn(score);

            // Act
            Score savedScore = scoreService.saveScore(score);

            // Assert
            assertNotNull(savedScore);
            assertEquals(player.getEmail(), savedScore.getPlayer().getEmail());
            assertEquals(score.getId(), savedScore.getId());
            verify(repo).save(score);
        }

        @Test
        void saveScore_scoreIsNull_throwsIllegalArgumentException() {
            // Act & Assert
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
            List<Score> content = List.of(mock(Score.class));
            Page<Score> expectedPage = new PageImpl<>(content, pageable, 1);

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
        void findScoreByPlayerId_scoreExists_returnsOptionalWithScore() {
            // Arrange
            Player player = getExamplePlayer();
            Score score = Score.builder()
                    .player(player)
                    .scoreTimestamp(Instant.now())
                    .numberOfTries(1)
                    .build();

            when(repo.findByPlayerId(player.getId())).thenReturn(Optional.of(score));

            // Act
            Optional<Score> result = scoreService.findScoreByPlayerId(player.getId());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(player.getId(), result.get().getPlayer().getId());
            verify(repo).findByPlayerId(player.getId());
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
    }

    @Nested
    class RegisterScoreTests {

        @Test
        void registerScore_validData_savesScoreAndEvictsLastPageCache() {
            // Arrange
            Player player = getExamplePlayer();
            int tries = 3;

            when(repo.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(repo.count()).thenReturn(25L); // 25 elements -> last page should be 2 ((25-1)/10)

            // Act
            Score result = scoreService.registerScore(player, tries);

            // Assert
            assertNotNull(result);
            assertEquals(player, result.getPlayer());
            assertEquals(tries, result.getNumberOfTries());
            assertNotNull(result.getScoreTimestamp());

            verify(repo).save(any(Score.class));
            verify(repo).count();
            verify(cacheService).evictCacheEntry("classic-leaderboard", 2);
        }
    }

    @Nested
    class RegisterScoreWithTimestampTests {

        @Test
        void registerScoreWithTimestamp_isHistoricalInsert_clearsClassicCacheAndEvictsTop3IfNeeded() {
            // Arrange
            Player player = getExamplePlayer();
            int tries = 3;
            Instant now = Instant.now();
            Instant historicalTimestamp = now.minusSeconds(3600); // 1 hour ago

            when(repo.findLatestScoreTimestamp()).thenReturn(Optional.of(now));
            when(repo.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Mock Top3 list to be smaller than 3 to force top-3 cache eviction
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of(mock(LeaderboardEntryDto.class)));

            // Act
            Score result = scoreService.registerScoreWithTimestamp(player, tries, historicalTimestamp);

            // Assert
            verify(repo).save(scoreCaptor.capture());
            assertEquals(historicalTimestamp, scoreCaptor.getValue().getScoreTimestamp());

            verify(cacheService).clear("classic-leaderboard");
            verify(cacheService, never()).evictCacheEntry(eq("classic-leaderboard"), anyInt());
            verify(cacheService).evictCache("top-3-leaderboard");
        }

        @Test
        void registerScoreWithTimestamp_isNotHistoricalInsert_evictsLastPageAndTop3IfNeeded() {
            // Arrange
            Player player = getExamplePlayer();
            int tries = 3;
            Instant now = Instant.now();
            Instant futureTimestamp = now.plusSeconds(3600);

            when(repo.findLatestScoreTimestamp()).thenReturn(Optional.of(now));
            when(repo.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(repo.count()).thenReturn(5L); // Last page will be 0

            // Mock Top3 list of size 3, where new timestamp is NOT after the 3rd element's timestamp (forcing eviction)
            LeaderboardEntryDto thirdEntry = mock(LeaderboardEntryDto.class);
            when(thirdEntry.scoreTimeStamp()).thenReturn(futureTimestamp.plusSeconds(100));
            when(leaderboardService.getTop3Leaderboard()).thenReturn(List.of(
                    mock(LeaderboardEntryDto.class),
                    mock(LeaderboardEntryDto.class),
                    thirdEntry
            ));

            // Act
            Score result = scoreService.registerScoreWithTimestamp(player, tries, futureTimestamp);

            // Assert
            verify(repo).save(any(Score.class));
            verify(cacheService, never()).clear("classic-leaderboard");
            verify(cacheService).evictCacheEntry("classic-leaderboard", 0);
            verify(cacheService).evictCache("top-3-leaderboard");
        }
    }

    @Nested
    class DeletionTests {

        @Test
        void deleteScoreById_validId_callsRepositoryDelete() {
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

        @Test
        void existsById_idDoesNotExist_returnsFalse() {
            // Arrange
            Long scoreId = 1L;
            when(repo.existsById(scoreId)).thenReturn(false);

            // Act
            boolean result = scoreService.existsById(scoreId);

            // Assert
            assertFalse(result);
            verify(repo).existsById(scoreId);
        }
    }
}