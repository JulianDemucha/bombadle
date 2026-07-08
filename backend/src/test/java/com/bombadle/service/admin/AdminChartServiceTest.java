package com.bombadle.service.admin;

import com.bombadle.dto.ActivityChartCombinedPointDto;
import com.bombadle.dto.ActivityChartSplitPointDto;
import com.bombadle.dto.AdminChartResponse;
import com.bombadle.dto.DailySolversChartCombinedPointDto;
import com.bombadle.dto.DailySolversChartSplitPointDto;
import com.bombadle.entity.ActivitySnapshot;
import com.bombadle.entity.DailySolverStatistic;
import com.bombadle.enums.ActivityChartDensity;
import com.bombadle.enums.ActivityChartPeriod;
import com.bombadle.enums.DailySolversChartDensity;
import com.bombadle.enums.DailySolversChartPeriod;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.ActivitySnapshotRepository;
import com.bombadle.repository.DailySolverStatisticRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminChartServiceTest {

    @Mock
    private ActivitySnapshotRepository activitySnapshotRepository;

    @Mock
    private DailySolverStatisticRepository dailySolverStatisticRepository;

    @InjectMocks
    private AdminChartService adminChartService;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    // --- helpers ---

    private ActivitySnapshot snapshot(String iso, int loggedIn, int anonymous) {
        return ActivitySnapshot.builder()
                .timestamp(Instant.parse(iso))
                .loggedInActiveCount(loggedIn)
                .anonymousActiveCount(anonymous)
                .build();
    }

    private DailySolverStatistic stat(LocalDate date, int totalSolvers, int totalAnonymous) {
        return DailySolverStatistic.builder()
                .gameMode(GameMode.CLASSIC)
                .puzzleDate(date)
                .totalSolvers(totalSolvers)
                .totalAnonymousSolvers(totalAnonymous)
                .capturedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // Endpoint A — activity chart
    // =========================================================================

    @Nested
    class ActivityChart {

        @Nested
        class TenMinutesDensity {

            @Test
            void split_eachSnapshotBecomesOnePoint_noAveraging() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T10:00:00Z", 3, 1),
                                snapshot("2024-01-15T11:00:00Z", 5, 2)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.LAST_DAY, false);

                List<?> points = response.points();
                assertEquals(2, points.size());

                ActivityChartSplitPointDto p0 = (ActivityChartSplitPointDto) points.get(0);
                assertEquals(Instant.parse("2024-01-15T10:00:00Z"), p0.x());
                assertEquals(3.0, p0.loggedIn());
                assertEquals(1.0, p0.anonymous());

                ActivityChartSplitPointDto p1 = (ActivityChartSplitPointDto) points.get(1);
                assertEquals(Instant.parse("2024-01-15T11:00:00Z"), p1.x());
                assertEquals(5.0, p1.loggedIn());
                assertEquals(2.0, p1.anonymous());
            }

            @Test
            void combined_eachSnapshotBecomesOnePoint_valueIsSum() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T10:00:00Z", 4, 6)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.LAST_DAY, true);

                ActivityChartCombinedPointDto point = (ActivityChartCombinedPointDto) response.points().get(0);
                assertEquals(Instant.parse("2024-01-15T10:00:00Z"), point.x());
                assertEquals(10.0, point.value());
            }

            @Test
            void split_emptyRepository_returnsEmptyPoints() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of());

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.LAST_DAY, false);

                assertTrue(response.points().isEmpty());
            }
        }

        @Nested
        class HourDensity {

            // All timestamps use January 2024 (CET = UTC+1).
            // 2024-01-15T12:10Z = Warsaw 13:10 -> truncated to 13:00 Warsaw -> bucket key 12:00Z
            // 2024-01-15T12:50Z = Warsaw 13:50 -> same bucket 12:00Z
            // 2024-01-15T13:10Z = Warsaw 14:10 -> bucket key 13:00Z

            @Test
            void split_twoSnapshotsInSameHour_averagedIntoOnePoint() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T12:10:00Z", 3, 1),
                                snapshot("2024-01-15T12:50:00Z", 5, 3)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_DAY, false);

                assertEquals(1, response.points().size());
                ActivityChartSplitPointDto point = (ActivityChartSplitPointDto) response.points().get(0);
                assertEquals(Instant.parse("2024-01-15T12:00:00Z"), point.x()); // 13:00 Warsaw = 12:00 UTC
                assertEquals(4.0, point.loggedIn(), 1e-9);  // avg(3, 5)
                assertEquals(2.0, point.anonymous(), 1e-9); // avg(1, 3)
            }

            @Test
            void split_snapshotsInDifferentHours_separatePoints() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T12:10:00Z", 3, 1),
                                snapshot("2024-01-15T13:10:00Z", 7, 3)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_DAY, false);

                assertEquals(2, response.points().size());

                ActivityChartSplitPointDto first = (ActivityChartSplitPointDto) response.points().get(0);
                assertEquals(Instant.parse("2024-01-15T12:00:00Z"), first.x());

                ActivityChartSplitPointDto second = (ActivityChartSplitPointDto) response.points().get(1);
                assertEquals(Instant.parse("2024-01-15T13:00:00Z"), second.x());
            }

            @Test
            void combined_twoSnapshotsInSameHour_averageOfSums() {
                // loggedIn+anonymous per snapshot: 3+1=4, 5+3=8 → avg = 6.0
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T12:10:00Z", 3, 1),
                                snapshot("2024-01-15T12:50:00Z", 5, 3)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_DAY, true);

                assertEquals(1, response.points().size());
                ActivityChartCombinedPointDto point = (ActivityChartCombinedPointDto) response.points().get(0);
                assertEquals(6.0, point.value(), 1e-9); // avg(4, 8)
            }

            @Test
            void split_pointsOrderedChronologically() {
                // Return from repo already sorted but groups might re-order — verify final sort
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T13:10:00Z", 7, 3),
                                snapshot("2024-01-15T12:10:00Z", 3, 1)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_DAY, false);

                assertEquals(2, response.points().size());
                Instant first = ((ActivityChartSplitPointDto) response.points().get(0)).x();
                Instant second = ((ActivityChartSplitPointDto) response.points().get(1)).x();
                assertTrue(first.isBefore(second), "Points must be in ascending order");
            }
        }

        @Nested
        class DayDensity {

            // CET (UTC+1, winter). Warsaw midnight for Jan 15 = 2024-01-14T23:00:00Z.
            // 2024-01-15T10:00Z = Warsaw 11:00 Jan 15 -> bucket 2024-01-14T23:00Z
            // 2024-01-15T14:00Z = Warsaw 15:00 Jan 15 -> same bucket
            // 2024-01-16T10:00Z = Warsaw 11:00 Jan 16 -> bucket 2024-01-15T23:00Z

            @Test
            void split_twoSnapshotsSameWarsawDay_averagedIntoOnePoint() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T10:00:00Z", 2, 4),
                                snapshot("2024-01-15T14:00:00Z", 6, 8)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.DAY, ActivityChartPeriod.LAST_WEEK, false);

                assertEquals(1, response.points().size());
                ActivityChartSplitPointDto point = (ActivityChartSplitPointDto) response.points().get(0);
                // Warsaw Jan 15 midnight in UTC
                assertEquals(Instant.parse("2024-01-14T23:00:00Z"), point.x());
                assertEquals(4.0, point.loggedIn(), 1e-9);  // avg(2, 6)
                assertEquals(6.0, point.anonymous(), 1e-9); // avg(4, 8)
            }

            @Test
            void split_snapshotsInDifferentWarsawDays_separatePoints() {
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T10:00:00Z", 2, 4),
                                snapshot("2024-01-16T10:00:00Z", 6, 8)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.DAY, ActivityChartPeriod.LAST_WEEK, false);

                assertEquals(2, response.points().size());
                assertEquals(Instant.parse("2024-01-14T23:00:00Z"),
                        ((ActivityChartSplitPointDto) response.points().get(0)).x());
                assertEquals(Instant.parse("2024-01-15T23:00:00Z"),
                        ((ActivityChartSplitPointDto) response.points().get(1)).x());
            }

            @Test
            void split_utcDayBoundaryDoesNotSplitWarsawDay() {
                // 2024-01-15T22:30Z is still Jan 15 in Warsaw (23:30 CET), so same bucket as 10:00Z
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of(
                                snapshot("2024-01-15T10:00:00Z", 2, 0),
                                snapshot("2024-01-15T22:30:00Z", 4, 0)
                        ));

                AdminChartResponse<?> response = adminChartService.getActivityChart(
                        ActivityChartDensity.DAY, ActivityChartPeriod.LAST_WEEK, false);

                // Both timestamps fall in Warsaw Jan 15 -> single bucket
                assertEquals(1, response.points().size());
                assertEquals(3.0, ((ActivityChartSplitPointDto) response.points().get(0)).loggedIn(), 1e-9);
            }
        }

        @Nested
        class PeriodFiltering {

            @Test
            void allPeriod_callsFindAll_notRanged() {
                when(activitySnapshotRepository.findAllByOrderByTimestampAsc()).thenReturn(List.of());

                adminChartService.getActivityChart(ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.ALL, false);

                verify(activitySnapshotRepository).findAllByOrderByTimestampAsc();
                verify(activitySnapshotRepository, never()).findByTimestampAfterOrderByTimestampAsc(any());
            }

            @Test
            void lastHourPeriod_callsRangedQuery_withApproximatelyOneHourCutoff() {
                Instant before = Instant.now().minus(1, ChronoUnit.HOURS);
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of());

                adminChartService.getActivityChart(ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.LAST_HOUR, false);

                verify(activitySnapshotRepository).findByTimestampAfterOrderByTimestampAsc(instantCaptor.capture());
                assertTrue(isWithin5Seconds(instantCaptor.getValue(), before),
                        "Cutoff should be ~1 hour ago");
            }

            @Test
            void lastWeekPeriod_callsRangedQuery_withApproximatelySevenDayCutoff() {
                Instant before = Instant.now().minus(7, ChronoUnit.DAYS);
                when(activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any()))
                        .thenReturn(List.of());

                adminChartService.getActivityChart(ActivityChartDensity.TEN_MINUTES, ActivityChartPeriod.LAST_WEEK, false);

                verify(activitySnapshotRepository).findByTimestampAfterOrderByTimestampAsc(instantCaptor.capture());
                assertTrue(isWithin5Seconds(instantCaptor.getValue(), before),
                        "Cutoff should be ~7 days ago");
            }

            private boolean isWithin5Seconds(Instant actual, Instant expected) {
                return Math.abs(Duration.between(actual, expected).toSeconds()) < 5;
            }
        }
    }

    // =========================================================================
    // Endpoint B — daily-solvers chart
    // =========================================================================

    @Nested
    class DailySolversChart {

        @Nested
        class QuotesStage1 {

            @Test
            void returnsEmptyResponse_withoutQueryingDatabase() {
                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.QUOTES_STAGE_1, false);

                assertTrue(response.points().isEmpty());
                verifyNoInteractions(dailySolverStatisticRepository);
            }
        }

        @Nested
        class DayDensityTests {

            @Test
            void split_eachRowBecomesOnePoint() {
                LocalDate date1 = LocalDate.of(2024, 6, 10);
                LocalDate date2 = LocalDate.of(2024, 6, 11);
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(
                                stat(date1, 10, 3),
                                stat(date2, 20, 5)
                        ));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertEquals(2, response.points().size());

                DailySolversChartSplitPointDto p0 = (DailySolversChartSplitPointDto) response.points().get(0);
                assertEquals(date1, p0.x());
                assertEquals(10.0, p0.loggedIn());
                assertEquals(3.0, p0.anonymous());

                DailySolversChartSplitPointDto p1 = (DailySolversChartSplitPointDto) response.points().get(1);
                assertEquals(date2, p1.x());
                assertEquals(20.0, p1.loggedIn());
                assertEquals(5.0, p1.anonymous());
            }

            @Test
            void combined_valueIsSumPerRow() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(stat(LocalDate.of(2024, 6, 10), 10, 4)));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, true);

                DailySolversChartCombinedPointDto point =
                        (DailySolversChartCombinedPointDto) response.points().get(0);
                assertEquals(LocalDate.of(2024, 6, 10), point.x());
                assertEquals(14.0, point.value());
            }

            @Test
            void emptyRows_returnsEmptyPoints() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        any(), any(), any())).thenReturn(List.of());

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertTrue(response.points().isEmpty());
            }
        }

        @Nested
        class WeekDensityTests {

            // 2024-01-15 = Monday. Week: Mon Jan 15 – Sun Jan 21.
            // 2024-01-22 = Monday (next week).

            @Test
            void split_twoRowsInSameIsoWeek_averagedIntoOnePoint() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(
                                stat(LocalDate.of(2024, 1, 15), 10, 2), // Monday
                                stat(LocalDate.of(2024, 1, 17), 20, 4)  // Wednesday, same week
                        ));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertEquals(1, response.points().size());
                DailySolversChartSplitPointDto point =
                        (DailySolversChartSplitPointDto) response.points().get(0);
                assertEquals(LocalDate.of(2024, 1, 15), point.x()); // week-start Monday
                assertEquals(15.0, point.loggedIn(), 1e-9);  // avg(10, 20)
                assertEquals(3.0, point.anonymous(), 1e-9);  // avg(2, 4)
            }

            @Test
            void split_rowsInDifferentWeeks_separatePoints() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(
                                stat(LocalDate.of(2024, 1, 15), 10, 2), // week of Jan 15
                                stat(LocalDate.of(2024, 1, 22), 30, 6)  // week of Jan 22
                        ));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertEquals(2, response.points().size());
                assertEquals(LocalDate.of(2024, 1, 15),
                        ((DailySolversChartSplitPointDto) response.points().get(0)).x());
                assertEquals(LocalDate.of(2024, 1, 22),
                        ((DailySolversChartSplitPointDto) response.points().get(1)).x());
            }

            @Test
            void split_xValueIsMonday_notTheOriginalDate() {
                // Wednesday Jan 17 belongs to the week starting Monday Jan 15
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(stat(LocalDate.of(2024, 1, 17), 10, 2)));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertEquals(1, response.points().size());
                assertEquals(LocalDate.of(2024, 1, 15),
                        ((DailySolversChartSplitPointDto) response.points().get(0)).x());
            }

            @Test
            void combined_twoRowsInSameWeek_averageOfSums() {
                // (10+2)=12, (20+4)=24 -> avg = 18
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(
                                stat(LocalDate.of(2024, 1, 15), 10, 2),
                                stat(LocalDate.of(2024, 1, 17), 20, 4)
                        ));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, true);

                assertEquals(1, response.points().size());
                DailySolversChartCombinedPointDto point =
                        (DailySolversChartCombinedPointDto) response.points().get(0);
                assertEquals(LocalDate.of(2024, 1, 15), point.x());
                assertEquals(18.0, point.value(), 1e-9);
            }

            @Test
            void split_pointsOrderedChronologicallyByWeekStart() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.CLASSIC), any(), any()))
                        .thenReturn(List.of(
                                stat(LocalDate.of(2024, 1, 22), 30, 6),
                                stat(LocalDate.of(2024, 1, 15), 10, 2)
                        ));

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                assertEquals(2, response.points().size());
                LocalDate first = ((DailySolversChartSplitPointDto) response.points().get(0)).x();
                LocalDate second = ((DailySolversChartSplitPointDto) response.points().get(1)).x();
                assertTrue(first.isBefore(second), "Points must be in ascending order");
            }
        }

        @Nested
        class PeriodFiltering {

            @Test
            void passesCorrectGameModeToRepository() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        any(), any(), any())).thenReturn(List.of());

                adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.IMAGES, false);

                verify(dailySolverStatisticRepository)
                        .findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                                eq(GameMode.IMAGES), any(), any());
            }

            @Test
            void lastMonthPeriod_fromDateIsOneMonthBefore() {
                ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
                ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        any(), any(), any())).thenReturn(List.of());

                adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.CLASSIC, false);

                verify(dailySolverStatisticRepository)
                        .findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                                any(), fromCaptor.capture(), toCaptor.capture());

                LocalDate capturedTo = toCaptor.getValue();
                LocalDate capturedFrom = fromCaptor.getValue();
                assertEquals(capturedTo.minusMonths(1), capturedFrom,
                        "from should be exactly one month before today");
            }

            @Test
            void last6MonthsPeriod_fromDateIsSixMonthsBefore() {
                ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
                ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        any(), any(), any())).thenReturn(List.of());

                adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_6_MONTHS,
                        GameMode.CLASSIC, false);

                verify(dailySolverStatisticRepository)
                        .findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                                any(), fromCaptor.capture(), toCaptor.capture());

                assertEquals(toCaptor.getValue().minusMonths(6), fromCaptor.getValue());
            }

            @Test
            void quotesStage2_queriesRepositoryNormally() {
                when(dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        eq(GameMode.QUOTES_STAGE_2), any(), any())).thenReturn(List.of());

                AdminChartResponse<?> response = adminChartService.getDailySolversChart(
                        DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                        GameMode.QUOTES_STAGE_2, false);

                assertTrue(response.points().isEmpty());
                verify(dailySolverStatisticRepository)
                        .findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                                eq(GameMode.QUOTES_STAGE_2), any(), any());
            }
        }
    }
}
