package com.bombadle.service.stats;

import com.bombadle.dto.FullLeaderboardEntryDto;
import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.StreakLeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.ScoreNotFoundException;
import com.bombadle.repository.PlayerRepository;
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

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private static Player playerWithStreaks(Long id, int currentStreak, int currentSuperstreak) {
        return Player.builder()
                .id(id)
                .displayName("player-" + id)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .currentStreak(currentStreak)
                .currentSuperstreak(currentSuperstreak)
                .build();
    }

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
            Page<FullLeaderboardEntryDto> expectedPage = new PageImpl<>(List.of(mock(FullLeaderboardEntryDto.class)));
            when(repo.findPagedLeaderboard(mode, PageRequest.of(page, 10))).thenReturn(expectedPage);

            // Act
            Page<FullLeaderboardEntryDto> result = leaderboardService.getPagedLeaderboard(mode, page);

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

    @Nested
    class GetStreakTop3Tests {

        @Test
        void getStreakTop3_assignsRanksByPositionAndExcludesZeroStreak() {
            // Arrange
            Player first = playerWithStreaks(10L, 7, 3);
            Player second = playerWithStreaks(20L, 5, 1);
            when(playerRepository
                    .findTop3ByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0))
                    .thenReturn(List.of(first, second));

            // Act
            List<StreakLeaderboardEntryDto> result = leaderboardService.getStreakTop3();

            // Assert
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).rank());
            assertEquals(10L, result.get(0).playerId());
            assertEquals(7, result.get(0).currentStreak());
            assertEquals(2L, result.get(1).rank());
            assertEquals(20L, result.get(1).playerId());
            verify(playerRepository)
                    .findTop3ByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0);
        }
    }

    @Nested
    class GetSuperstreakTop3Tests {

        @Test
        void getSuperstreakTop3_assignsRanksByPositionAndExcludesZeroStreak() {
            // Arrange
            Player first = playerWithStreaks(30L, 4, 9);
            Player second = playerWithStreaks(40L, 2, 6);
            when(playerRepository
                    .findTop3ByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0))
                    .thenReturn(List.of(first, second));

            // Act
            List<StreakLeaderboardEntryDto> result = leaderboardService.getSuperstreakTop3();

            // Assert
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).rank());
            assertEquals(9, result.get(0).currentSuperstreak());
            assertEquals(2L, result.get(1).rank());
            assertEquals(40L, result.get(1).playerId());
            verify(playerRepository)
                    .findTop3ByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0);
        }
    }

    @Nested
    class GetStreakPagedLeaderboardTests {

        @Test
        void getStreakPagedLeaderboard_assignsOffsetBasedRanksAndPreservesTotal() {
            // Arrange
            int page = 2;
            PageRequest pageable = PageRequest.of(page, 10);
            Player first = playerWithStreaks(50L, 6, 2);
            Player second = playerWithStreaks(60L, 6, 1);
            Page<Player> playerPage = new PageImpl<>(List.of(first, second), pageable, 42);
            when(playerRepository
                    .findByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0, pageable))
                    .thenReturn(playerPage);

            // Act
            Page<StreakLeaderboardEntryDto> result = leaderboardService.getStreakPagedLeaderboard(page);

            // Assert
            assertEquals(42, result.getTotalElements());
            assertEquals(21L, result.getContent().get(0).rank()); // offset 20 + 1
            assertEquals(22L, result.getContent().get(1).rank());
            assertEquals(50L, result.getContent().get(0).playerId());
            verify(playerRepository)
                    .findByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0, pageable);
        }
    }

    @Nested
    class GetSuperstreakPagedLeaderboardTests {

        @Test
        void getSuperstreakPagedLeaderboard_assignsOffsetBasedRanksAndPreservesTotal() {
            // Arrange
            int page = 0;
            PageRequest pageable = PageRequest.of(page, 10);
            Player first = playerWithStreaks(70L, 1, 8);
            Page<Player> playerPage = new PageImpl<>(List.of(first), pageable, 1);
            when(playerRepository
                    .findByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0, pageable))
                    .thenReturn(playerPage);

            // Act
            Page<StreakLeaderboardEntryDto> result = leaderboardService.getSuperstreakPagedLeaderboard(page);

            // Assert
            assertEquals(1, result.getTotalElements());
            assertEquals(1L, result.getContent().get(0).rank());
            assertEquals(8, result.getContent().get(0).currentSuperstreak());
            verify(playerRepository)
                    .findByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0, pageable);
        }
    }
}