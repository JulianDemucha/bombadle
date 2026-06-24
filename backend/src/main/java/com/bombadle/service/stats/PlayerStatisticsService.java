package com.bombadle.service.stats;

import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStatisticsService {

    /** Same calendar-day boundary as {@code DailyResetService}: the daily reset runs at 07:00 Europe/Warsaw. */
    static final ZoneId RESET_ZONE = ZoneId.of("Europe/Warsaw");
    static final int RESET_HOUR = 7;

    private final PlayerDailyStatisticRepository playerDailyStatisticRepository;
    private final LeaderboardService leaderboardService;

    /**
     * Persists a permanent, per-day snapshot of a solved puzzle, mirroring the {@link Score}
     * that is wiped by the daily reset.
     * <p>
     * The leaderboard position is captured here, at solve time, on purpose: it reflects the
     * position the player actually saw. Capturing it later (e.g. at the daily reset) would drift
     * as other players post better times for the rest of the day.
     * <p>
     * {@code QUOTES_STAGE_1} is intentionally skipped — it is not a ranked board, so a position
     * snapshot would be meaningless.
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
     * Resolves the puzzle day for a given solve instant using the same 07:00 Europe/Warsaw
     * boundary as the daily reset. A solve before 07:00 local time belongs to the previous
     * puzzle day (the one that has not been reset yet).
     */
    LocalDate resolvePuzzleDate(Instant solvedAt) {
        return solvedAt.atZone(RESET_ZONE).minusHours(RESET_HOUR).toLocalDate();
    }
}
