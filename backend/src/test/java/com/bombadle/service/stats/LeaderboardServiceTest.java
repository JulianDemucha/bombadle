package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Score;
import com.bombadle.exception.ScoreNotFoundException;
import com.bombadle.repository.ScoreRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private ScoreRepository repo;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Nested
    class GetTop3LeaderboardTests {

        @Test
        void getTop3Leaderboard_called_returnsListOfDtos() {
            // Arrange
            List<LeaderboardEntryDto> expectedList = List.of(mock(LeaderboardEntryDto.class));
            when(repo.findTop3()).thenReturn(expectedList);

            // Act
            List<LeaderboardEntryDto> result = leaderboardService.getTop3Leaderboard();

            // Assert
            assertEquals(expectedList, result);
            verify(repo).findTop3();
        }
    }

    @Nested
    class GetPagedLeaderboardTests {

        @Test
        void getPagedLeaderboard_validPage_returnsPageOfDtos() {
            // Arrange
            int page = 0;
            List<LeaderboardEntryDto> content = List.of(mock(LeaderboardEntryDto.class));
            Page<LeaderboardEntryDto> expectedPage = new org.springframework.data.domain.PageImpl<>(content);
            when(repo.findPagedLeaderboard(any(PageRequest.class))).thenReturn(expectedPage);

            // Act
            Page<LeaderboardEntryDto> result = leaderboardService.getPagedLeaderboard(page);

            // Assert
            assertEquals(expectedPage, result);
            verify(repo).findPagedLeaderboard(PageRequest.of(page, 10));
        }
    }

    @Nested
    class GetTop10LeaderboardTests {

        @Test
        void getTop10Leaderboard_called_returnsListOfScores() {
            // Arrange
            List<Score> expectedList = List.of(mock(Score.class));
            when(repo.findTop10ByOrderByScoreTimestampAsc()).thenReturn(expectedList);

            // Act
            List<Score> result = leaderboardService.getTop10Leaderboard();

            // Assert
            assertEquals(expectedList, result);
            verify(repo).findTop10ByOrderByScoreTimestampAsc();
        }
    }

    @Nested
    class GetPlayerRankByIdTests {

        @Test
        void getPlayerRankById_scoreExists_returnsRank() {
            // Arrange
            Long playerId = 1L;
            Long expectedRank = 5L;
            Score score = mock(Score.class);

            when(repo.findByPlayerId(playerId)).thenReturn(Optional.of(score));
            when(repo.findRankByPlayerId(playerId)).thenReturn(expectedRank);

            // Act
            Long result = leaderboardService.getPlayerRankById(playerId);

            // Assert
            assertEquals(expectedRank, result);
            verify(repo).findByPlayerId(playerId);
            verify(repo).findRankByPlayerId(playerId);
        }

        @Test
        void getPlayerRankById_scoreDoesNotExist_throwsScoreNotFoundException() {
            // Arrange
            Long playerId = 1L;
            when(repo.findByPlayerId(playerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ScoreNotFoundException.class, () -> leaderboardService.getPlayerRankById(playerId));
            verify(repo).findByPlayerId(playerId);
            verify(repo, never()).findRankByPlayerId(anyLong());
        }
    }

    @Nested
    class GetRankedEntryByPlayerIdTests {

        @Test
        void getRankedEntryByPlayerId_entryExists_returnsDto() {
            // Arrange
            Long playerId = 1L;
            LeaderboardEntryDto expectedDto = mock(LeaderboardEntryDto.class);
            when(repo.findLeaderboardRankedEntryByPlayerId(playerId)).thenReturn(Optional.of(expectedDto));

            // Act
            LeaderboardEntryDto result = leaderboardService.getRankedEntryByPlayerId(playerId);

            // Assert
            assertEquals(expectedDto, result);
            verify(repo).findLeaderboardRankedEntryByPlayerId(playerId);
        }

        @Test
        void getRankedEntryByPlayerId_entryDoesNotExist_throwsScoreNotFoundException() {
            // Arrange
            Long playerId = 1L;
            when(repo.findLeaderboardRankedEntryByPlayerId(playerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ScoreNotFoundException.class, () -> leaderboardService.getRankedEntryByPlayerId(playerId));
            verify(repo).findLeaderboardRankedEntryByPlayerId(playerId);
        }
    }

    @Nested
    class GetLatestScoreTests {

        @Test
        void getLatestScore_scoreExists_returnsOptionalWithScore() {
            // Arrange
            Score expectedScore = mock(Score.class);
            when(repo.findTopByOrderByScoreTimestampDesc()).thenReturn(Optional.of(expectedScore));

            // Act
            Optional<Score> result = leaderboardService.getLatestScore();

            // Assert
            assertTrue(result.isPresent());
            assertEquals(expectedScore, result.get());
            verify(repo).findTopByOrderByScoreTimestampDesc();
        }

        @Test
        void getLatestScore_noScores_returnsEmptyOptional() {
            // Arrange
            when(repo.findTopByOrderByScoreTimestampDesc()).thenReturn(Optional.empty());

            // Act
            Optional<Score> result = leaderboardService.getLatestScore();

            // Assert
            assertTrue(result.isEmpty());
            verify(repo).findTopByOrderByScoreTimestampDesc();
        }
    }
}
