package com.bombadle.integration;

import com.bombadle.dto.DailyStatisticDto;
import com.bombadle.entity.*;
import com.bombadle.enums.*;
import com.bombadle.repository.DailySolverStatisticRepository;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.stats.PlayerStatisticsService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Transactional
class PlayerStatisticsChartIT extends BaseIT {

    @Autowired
    private PlayerStatisticsService playerStatisticsService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PlayerDailyStatisticRepository playerDailyStatisticRepository;
    @Autowired
    private DailySolverStatisticRepository dailySolverStatisticRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void getChartStatistics_joinsAggregateForPercentile_andNullWhenNoAggregateRow() {
        Player player = playerRepository.save(Player.builder()
                .email("chart@bombadle.com")
                .login("chart")
                .passwordHash("hash")
                .role(Role.ROLE_USER)
                .totalSuccessfulGuesses(0)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .accountLocked(false)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .emailVerified(true)
                .build());

        playerDailyStatisticRepository.save(PlayerDailyStatistic.builder()
                .player(player)
                .gameMode(GameMode.CLASSIC)
                .puzzleDate(LocalDate.of(2026, 6, 24))
                .solvedAt(Instant.parse("2026-06-24T12:00:00Z"))
                .numberOfTries(3)
                .leaderboardPosition(5)
                .totalParticipants(20)
                .build());
        dailySolverStatisticRepository.save(DailySolverStatistic.builder()
                .gameMode(GameMode.CLASSIC)
                .puzzleDate(LocalDate.of(2026, 6, 24))
                .totalSolvers(100)
                .totalAnonymousSolvers(7)
                .capturedAt(Instant.now())
                .build());

        playerDailyStatisticRepository.save(PlayerDailyStatistic.builder()
                .player(player)
                .gameMode(GameMode.IMAGES)
                .puzzleDate(LocalDate.of(2026, 6, 25))
                .solvedAt(Instant.parse("2026-06-25T12:00:00Z"))
                .numberOfTries(2)
                .leaderboardPosition(1)
                .totalParticipants(8)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<DailyStatisticDto> result = playerStatisticsService.getChartStatistics(player.getId());

        assertEquals(2, result.size());

        DailyStatisticDto closed = result.getFirst();
        assertEquals(GameMode.CLASSIC, closed.gameMode());
        assertEquals("2026-06-24", closed.puzzleDate());
        assertEquals(5, closed.leaderboardPosition());
        assertNotNull(closed.percentile());
        assertEquals(0.05, closed.percentile(), 1e-9);

        DailyStatisticDto inProgress = result.get(1);
        assertEquals(GameMode.IMAGES, inProgress.gameMode());
        assertEquals("2026-06-25", inProgress.puzzleDate());
        assertNull(inProgress.percentile());
    }
}
