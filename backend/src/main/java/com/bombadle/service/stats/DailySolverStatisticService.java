package com.bombadle.service.stats;

import com.bombadle.dto.TodaySolversDto;
import com.bombadle.entity.DailySolverStatistic;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.DailySolverStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures the finalized end-of-day solver totals (one {@link DailySolverStatistic} row per ranked
 * mode) for the puzzle day that is closing. Invoked once by the 07:00 daily reset.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailySolverStatisticService {

    private final TodaySolversService todaySolversService;
    private final DailySolverStatisticRepository dailySolverStatisticRepository;
    private final PlayerStatisticsService playerStatisticsService;

    /**
     * Persists the per-mode solver totals for the puzzle day that is ending.
     * <p>
     * Must run during the 07:00 reset AFTER {@code evaluateDailyStreaks()} but BEFORE the reset clears
     * {@code completed_modes_today} (the counts are read from it). Cost is O(number of ranked modes):
     * two count queries per mode (via {@link TodaySolversService}) plus a single batch insert — no
     * per-player iteration.
     */
    @Transactional
    public void captureClosingDay() {
        // At the 07:00 reset instant resolvePuzzleDate() returns the NEW day, so the day being closed
        // is the previous one. That day is also the puzzleDate already carried by today's
        // PlayerDailyStatistic rows, which is what the percentile join needs to line up.
        LocalDate closingPuzzleDate = playerStatisticsService.resolvePuzzleDate(Instant.now()).minusDays(1);
        Instant capturedAt = Instant.now();

        List<DailySolverStatistic> rows = new ArrayList<>();
        for (GameMode gameMode : GameMode.values()) {
            // Mirror recordDailyStatistic: QUOTES_STAGE_1 is not a ranked board, so it has no
            // PlayerDailyStatistic rows and needs no aggregate to join against.
            if (gameMode == GameMode.QUOTES_STAGE_1) {
                continue;
            }

            TodaySolversDto solvers = todaySolversService.getTodaySolvers(gameMode);
            boolean hadActivity = solvers.loggedIn() > 0 || solvers.anonymous() > 0;
            if (!hadActivity) {
                continue;
            }

            if (dailySolverStatisticRepository.existsByGameModeAndPuzzleDate(gameMode, closingPuzzleDate)) {
                log.debug("Daily solver statistic already exists for mode {} on {} - skipping",
                        gameMode, closingPuzzleDate);
                continue;
            }

            rows.add(DailySolverStatistic.builder()
                    .gameMode(gameMode)
                    .puzzleDate(closingPuzzleDate)
                    .totalSolvers((int) solvers.loggedIn())
                    .totalAnonymousSolvers((int) solvers.anonymous())
                    .capturedAt(capturedAt)
                    .build());
        }

        dailySolverStatisticRepository.saveAll(rows);
        log.info("Captured daily solver statistics for {} mode(s) on {}", rows.size(), closingPuzzleDate);
    }
}
