package com.bombadle.service.game;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.player.PlayerService;
import com.bombadle.service.stats.LeaderboardService;
import com.bombadle.service.stats.PlayerStatisticsService;
import com.bombadle.service.stats.ScoreService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreRegistrationService {
    private final ScoreService scoreService;
    private final PlayerService playerService;
    private final CacheService cacheService;
    private final LeaderboardService leaderboardService;
    private final PlayerStatisticsService playerStatisticsService;

    @Transactional
    public void registerPlayerWin(Long playerId, int numberOfTries, GameMode gameMode) {
        Player player = playerService.getPlayerById(playerId);

        Score score = Score.builder()
                .player(player)
                .scoreTimestamp(Instant.now())
                .numberOfTries(numberOfTries)
                .gameMode(gameMode)
                .build();

        Score savedScore = scoreService.save(score);

        player.addTodayScore(gameMode, savedScore);
        playerService.save(player);

        playerStatisticsService.recordDailyStatistic(player, savedScore);

        clearLeaderboardCaches(gameMode, Instant.now());
    }

    @Transactional
    public Score registerPlayerWinWithTimestamp(Long playerId, int numberOfTries, GameMode gameMode, Instant timestamp) {
        Player player = playerService.getPlayerById(playerId);

        Score score = Score.builder()
                .player(player)
                .scoreTimestamp(timestamp)
                .numberOfTries(numberOfTries)
                .gameMode(gameMode) // POPRAWKA: Dodano brakujący tryb gry
                .build();

        Score savedScore = scoreService.save(score);

        player.addTodayScore(gameMode, savedScore);
        playerService.save(player);

        playerStatisticsService.recordDailyStatistic(player, savedScore);

        clearLeaderboardCaches(gameMode, timestamp);

        return savedScore;
    }

    private void clearLeaderboardCaches(GameMode gameMode, Instant newTimestamp) {
        cacheService.clear("paged-leaderboard");

        List<LeaderboardEntryDto> currentTop3 = leaderboardService.getTop3Leaderboard(gameMode);

        if (currentTop3.size() < 3 || !newTimestamp.isAfter(currentTop3.get(2).scoreTimeStamp())) {
            cacheService.evictCacheEntry("top-3-leaderboard", gameMode.name());
        }
    }
}