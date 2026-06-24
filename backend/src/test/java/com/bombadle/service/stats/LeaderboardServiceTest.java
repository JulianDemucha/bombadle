package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.ScoreNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        void getTop3Leaderboard_validGameMode_returnsListOfDtos() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            List<LeaderboardEntryDto> expectedList = List.of(mock(LeaderboardEntryDto.class));
            when(repo.findTop3(mode)).thenReturn(expectedList);

            // Act
            List<LeaderboardEntryDto> result = leaderboardService.getTop3Leaderboard(mode);

            // Assert
            assertEquals(expectedList, result);
            verify(repo).findTop3(mode);
        }
    }

    @Nested
    class GetPagedLeaderboardTests {

        @Test
        void getPagedLeaderboard_validGameModeAndPage_returnsPageOfDtos() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            int page = 0;
            Page<LeaderboardEntryDto> expectedPage = new PageImpl<>(List.of(mock(LeaderboardEntryDto.class)));
            when(repo.findPagedLeaderboard(mode, PageRequest.of(page, 10))).thenReturn(expectedPage);

            // Act
            Page<LeaderboardEntryDto> result = leaderboardService.getPagedLeaderboard(mode, page);

            // Assert
            assertEquals(expectedPage, result);
            verify(repo).findPagedLeaderboard(mode, PageRequest.of(page, 10));
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
            GameMode mode = GameMode.CLASSIC;
            Long playerId = 1L;
            Long expectedRank = 5L;
            Score score = mock(Score.class);

            when(repo.findByPlayerIdAndGameMode(playerId, mode)).thenReturn(Optional.of(score));
            when(repo.findRankByPlayerId(mode, playerId)).thenReturn(expectedRank);

            // Act
            Long result = leaderboardService.getPlayerRankById(mode, playerId);

            // Assert
            assertEquals(expectedRank, result);
            verify(repo).findByPlayerIdAndGameMode(playerId, mode);
            verify(repo).findRankByPlayerId(mode, playerId);
        }

        @Test
        void getPlayerRankById_scoreDoesNotExist_throwsScoreNotFoundException() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            Long playerId = 1L;
            when(repo.findByPlayerIdAndGameMode(playerId, mode)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(ScoreNotFoundException.class, () -> leaderboardService.getPlayerRankById(mode, playerId));
            verify(repo).findByPlayerIdAndGameMode(playerId, mode);
            verify(repo, never()).findRankByPlayerId(any(), anyLong());
        }
    }

    @Nested
    class GetRankedEntryByPlayerIdTests {

        @Test
        void getRankedEntryByPlayerId_entryExists_returnsDto() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            Long playerId = 1L;
            LeaderboardEntryDto expectedDto = mock(LeaderboardEntryDto.class);
            when(repo.findLeaderboardRankedEntryByPlayerId(mode, playerId)).thenReturn(Optional.of(expectedDto));

            // Act
            LeaderboardEntryDto result = leaderboardService.getRankedEntryByPlayerId(mode, playerId);

            // Assert
            assertEquals(expectedDto, result);
            verify(repo).findLeaderboardRankedEntryByPlayerId(mode, playerId);
        }

        @Test
        void getRankedEntryByPlayerId_entryDoesNotExist_throwsScoreNotFoundException() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            Long playerId = 1L;
            when(repo.findLeaderboardRankedEntryByPlayerId(mode, playerId)).thenReturn(Optional.empty());

            // Act
            // Assert
            assertThrows(ScoreNotFoundException.class, () -> leaderboardService.getRankedEntryByPlayerId(mode, playerId));
            verify(repo).findLeaderboardRankedEntryByPlayerId(mode, playerId);
        }
    }

    @Nested
    class CountParticipantsTests {

        @Test
        void countParticipants_returnsRepositoryCountAsInt() {
            // Arrange
            GameMode mode = GameMode.CLASSIC;
            when(repo.countByGameMode(mode)).thenReturn(42L);

            // Act
            int result = leaderboardService.countParticipants(mode);

            // Assert
            assertEquals(42, result);
            verify(repo).countByGameMode(mode);
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