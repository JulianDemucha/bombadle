package com.bombadle.service.stats;

import com.bombadle.dto.BasicStatisticsDto;
import com.bombadle.dto.DailyStatisticDto;
import com.bombadle.dto.DetailedStatisticsDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import com.bombadle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStatisticsService {

    static final ZoneId RESET_ZONE = ZoneId.of("Europe/Warsaw");
    static final int RESET_HOUR = 7;

    /** A superstreak requires completing every game mode on a given day. */
    static final Set<GameMode> ALL_GAME_MODES = Player.ALL_GAME_MODES;

    private final PlayerDailyStatisticRepository playerDailyStatisticRepository;
    private final PlayerRepository playerRepository;
    private final LeaderboardService leaderboardService;

    /**
     * Persists a permanent, per-day snapshot of a solved puzzle, mirroring the {@link Score}
     * that is wiped by the daily reset.
     * {@code QUOTES_STAGE_1} is intentionally skipped — it is not a ranked board, so a position snapshot would be meaningless.
     */
    @Transactional
    public void recordDailyStatistic(Player player, Score score) {
        GameMode gameMode = score.getGameMode();
        if (gameMode == GameMode.QUOTES_STAGE_1) {
            return;
        }

        LocalDate puzzleDate = resolvePuzzleDate(score.getScoreTimestamp());

        if (playerDailyStatisticRepository.existsByPlayerIdAndGameModeAndPuzzleDate(player.getId(), gameMode, puzzleDate)) {
            log.debug("Daily statistic already exists for player {} in mode {} on {} - skipping",
                    player.getId(), gameMode, puzzleDate);
            return;
        }

        long leaderboardPosition = leaderboardService.getPlayerRankById(gameMode, player.getId());
        int totalParticipants = leaderboardService.countParticipants(gameMode);

        PlayerDailyStatistic statistic = PlayerDailyStatistic.builder()
                .player(player)
                .gameMode(gameMode)
                .puzzleDate(puzzleDate)
                .solvedAt(score.getScoreTimestamp())
                .numberOfTries(score.getNumberOfTries())
                .leaderboardPosition((int) leaderboardPosition)
                .totalParticipants(totalParticipants)
                .build();

        playerDailyStatisticRepository.save(statistic);
        log.debug("Recorded daily statistic for player {} in mode {} on {} (position {}/{})",
                player.getId(), gameMode, puzzleDate, leaderboardPosition, totalParticipants);
    }

    /**
     * Advances every player's streak counters for the puzzle day that has just ended.
     * <p>
     * Must run once per day, BEFORE the daily reset clears {@code completedModesToday} (the reset
     * wipes it with a bulk native update that bypasses the persistence context, so the in-memory
     * values must be read first). The 07:00 Europe/Warsaw day boundary is implicit: this is invoked
     * by the daily reset at that moment.
     */
    @Transactional
    public void evaluateDailyStreaks() {
        List<Player> players = playerRepository.findAll();

        for (Player player : players) {
            Set<GameMode> completed = player.getCompletedModesToday();
            boolean playedToday = completed != null && !completed.isEmpty();
            boolean completedAllModes = completed != null && completed.containsAll(ALL_GAME_MODES);
            player.resetStreaksIfThresholdsNotMet(playedToday, completedAllModes);
        }

        playerRepository.saveAll(players);
        playerRepository.flush();
        log.info("Evaluated daily streaks for {} players", players.size());
    }

    /** Compact statistics for the settings card. */
    @Transactional(readOnly = true)
    public BasicStatisticsDto getBasicStatistics(long playerId) {
        Player player = getPlayerOrThrow(playerId);
        return new BasicStatisticsDto(
                player.getCurrentSuperstreak(),
                player.getTotalSuccessfulGuesses(),
                (int) playerDailyStatisticRepository.countTop3FinishesByPlayerId(playerId)
        );
    }

    /** Full statistics for the dedicated statistics page. */
    @Transactional(readOnly = true)
    public DetailedStatisticsDto getDetailedStatistics(long playerId) {
        Player player = getPlayerOrThrow(playerId);

        Map<GameMode, Integer> guessesByMode = new EnumMap<>(GameMode.class);
        for (Object[] row : playerDailyStatisticRepository.countByPlayerIdGroupedByGameMode(playerId)) {
            guessesByMode.put((GameMode) row[0], ((Number) row[1]).intValue());
        }

        return new DetailedStatisticsDto(
                player.getTotalSuccessfulGuesses(),
                guessesByMode,
                player.getCurrentStreak(),
                player.getLongestStreak(),
                player.getCurrentSuperstreak(),
                player.getLongestSuperstreak()
        );
    }

    /**
     * Historical per-day solves for charting, ordered chronologically. Each solve's leaderboard
     * percentile is computed against the finalized end-of-day solver count (see
     * {@link PlayerDailyStatisticRepository#findChartRowsByPlayerId}); it is {@code null} for the
     * current in-progress day, which has no aggregate row yet.
     */
    @Transactional(readOnly = true)
    public List<DailyStatisticDto> getChartStatistics(long playerId) {
        return playerDailyStatisticRepository.findChartRowsByPlayerId(playerId)
                .stream()
                .map(row -> {
                    PlayerDailyStatistic statistic = (PlayerDailyStatistic) row[0];
                    Number totalSolvers = (Number) row[1];
                    Double percentile = (totalSolvers == null || totalSolvers.intValue() == 0)
                            ? null
                            : statistic.getLeaderboardPosition() * 1.0 / totalSolvers.intValue();
                    return DailyStatisticDto.toDto(statistic, percentile);
                })
                .toList();
    }

    /**
     * Removes all daily statistics for a player. Required for referential integrity when the
     * player is cascade-deleted (the table has a foreign key to {@code player}).
     */
    @Transactional
    public void deleteAllByPlayerId(Long playerId) {
        playerDailyStatisticRepository.deleteByPlayerId(playerId);
    }

    private Player getPlayerOrThrow(long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException("Player not found: " + playerId));
    }

    /**
     * Resolves the puzzle day for a given solve instant using the same 07:00 Europe/Warsaw
     * boundary as the daily reset. A solve before 07:00 local time belongs to the previous
     * puzzle day (the one that has not been reset yet).
     */
    LocalDate resolvePuzzleDate(Instant solvedAt) {
        return solvedAt.atZone(RESET_ZONE).minusHours(RESET_HOUR).toLocalDate();
    }
}
