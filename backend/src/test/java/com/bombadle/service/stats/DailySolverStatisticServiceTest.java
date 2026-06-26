package com.bombadle.service.stats;

import com.bombadle.dto.TodaySolversDto;
import com.bombadle.entity.DailySolverStatistic;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.DailySolverStatisticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailySolverStatisticServiceTest {

    @Mock
    private TodaySolversService todaySolversService;
    @Mock
    private DailySolverStatisticRepository dailySolverStatisticRepository;
    @Mock
    private PlayerStatisticsService playerStatisticsService;

    @InjectMocks
    private DailySolverStatisticService dailySolverStatisticService;

    @Captor
    private ArgumentCaptor<List<DailySolverStatistic>> rowsCaptor;

    @Test
    void captureClosingDay_writesOneRowPerActiveRankedMode_forThePreviousPuzzleDay() {
        // resolvePuzzleDate() at the 07:00 reset returns the NEW day; the closing day is the day before.
        when(playerStatisticsService.resolvePuzzleDate(any())).thenReturn(LocalDate.of(2026, 6, 26));
        when(todaySolversService.getTodaySolvers(GameMode.CLASSIC)).thenReturn(new TodaySolversDto(5, 2));
        when(todaySolversService.getTodaySolvers(GameMode.QUOTES_STAGE_2)).thenReturn(new TodaySolversDto(3, 1));
        when(todaySolversService.getTodaySolvers(GameMode.IMAGES)).thenReturn(new TodaySolversDto(0, 0)); // no activity
        when(dailySolverStatisticRepository.existsByGameModeAndPuzzleDate(any(), any())).thenReturn(false);

        dailySolverStatisticService.captureClosingDay();

        // QUOTES_STAGE_1 is not a ranked board -> never queried (mirrors recordDailyStatistic)
        verify(todaySolversService, never()).getTodaySolvers(GameMode.QUOTES_STAGE_1);

        verify(dailySolverStatisticRepository).saveAll(rowsCaptor.capture());
        List<DailySolverStatistic> rows = rowsCaptor.getValue();

        // IMAGES skipped (no activity), QUOTES_STAGE_1 skipped (not ranked)
        assertEquals(2, rows.size());

        Map<GameMode, DailySolverStatistic> byMode = rows.stream()
                .collect(Collectors.toMap(DailySolverStatistic::getGameMode, r -> r));

        DailySolverStatistic classic = byMode.get(GameMode.CLASSIC);
        assertNotNull(classic);
        assertEquals(LocalDate.of(2026, 6, 25), classic.getPuzzleDate()); // off-by-one: 2026-06-26 minus 1
        assertEquals(5, classic.getTotalSolvers());
        assertEquals(2, classic.getTotalAnonymousSolvers());
        assertNotNull(classic.getCapturedAt());

        DailySolverStatistic quotes = byMode.get(GameMode.QUOTES_STAGE_2);
        assertNotNull(quotes);
        assertEquals(LocalDate.of(2026, 6, 25), quotes.getPuzzleDate());
        assertEquals(3, quotes.getTotalSolvers());
        assertEquals(1, quotes.getTotalAnonymousSolvers());
    }

    @Test
    void captureClosingDay_skipsModeThatAlreadyHasARow() {
        when(playerStatisticsService.resolvePuzzleDate(any())).thenReturn(LocalDate.of(2026, 6, 26));
        when(todaySolversService.getTodaySolvers(GameMode.CLASSIC)).thenReturn(new TodaySolversDto(5, 0));
        when(todaySolversService.getTodaySolvers(GameMode.QUOTES_STAGE_2)).thenReturn(new TodaySolversDto(0, 0));
        when(todaySolversService.getTodaySolvers(GameMode.IMAGES)).thenReturn(new TodaySolversDto(0, 0));
        when(dailySolverStatisticRepository.existsByGameModeAndPuzzleDate(GameMode.CLASSIC, LocalDate.of(2026, 6, 25)))
                .thenReturn(true);

        dailySolverStatisticService.captureClosingDay();

        verify(dailySolverStatisticRepository).saveAll(rowsCaptor.capture());
        assertTrue(rowsCaptor.getValue().isEmpty());
    }
}
