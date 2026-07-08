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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminChartService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ActivitySnapshotRepository activitySnapshotRepository;
    private final DailySolverStatisticRepository dailySolverStatisticRepository;

    @Transactional(readOnly = true)
    public AdminChartResponse<?> getActivityChart(
            ActivityChartDensity density,
            ActivityChartPeriod period,
            boolean combined
    ) {
        List<ActivitySnapshot> snapshots = loadSnapshots(period);
        return combined
                ? new AdminChartResponse<>(buildCombinedPoints(snapshots, density))
                : new AdminChartResponse<>(buildSplitPoints(snapshots, density));
    }

    private List<ActivitySnapshot> loadSnapshots(ActivityChartPeriod period) {
        if (period == ActivityChartPeriod.ALL) {
            return activitySnapshotRepository.findAllByOrderByTimestampAsc();
        }
        Instant from = switch (period) {
            case LAST_HOUR  -> Instant.now().minus(1, ChronoUnit.HOURS);
            case LAST_DAY   -> Instant.now().minus(1, ChronoUnit.DAYS);
            case LAST_WEEK  -> Instant.now().minus(7, ChronoUnit.DAYS);
            case LAST_MONTH -> Instant.now().minus(30, ChronoUnit.DAYS);
            case ALL        -> throw new IllegalStateException("handled above");
        };
        return activitySnapshotRepository.findByTimestampAfterOrderByTimestampAsc(from);
    }

    private Instant toBucketKey(Instant ts, ActivityChartDensity density) {
        return switch (density) {
            case TEN_MINUTES -> ts;
            case HOUR        -> ts.atZone(WARSAW).truncatedTo(ChronoUnit.HOURS).toInstant();
            case DAY         -> ts.atZone(WARSAW).toLocalDate().atStartOfDay(WARSAW).toInstant();
        };
    }

    private List<ActivityChartSplitPointDto> buildSplitPoints(
            List<ActivitySnapshot> snapshots, ActivityChartDensity density) {

        if (density == ActivityChartDensity.TEN_MINUTES) {
            return snapshots.stream()
                    .map(s -> new ActivityChartSplitPointDto(
                            s.getTimestamp(),
                            s.getLoggedInActiveCount(),
                            s.getAnonymousActiveCount()))
                    .toList();
        }

        return snapshots.stream()
                .collect(Collectors.groupingBy(s -> toBucketKey(s.getTimestamp(), density)))
                .entrySet().stream()
                .map(e -> {
                    List<ActivitySnapshot> bucket = e.getValue();
                    double avgLoggedIn  = bucket.stream().mapToInt(ActivitySnapshot::getLoggedInActiveCount).average().orElse(0.0);
                    double avgAnonymous = bucket.stream().mapToInt(ActivitySnapshot::getAnonymousActiveCount).average().orElse(0.0);
                    return new ActivityChartSplitPointDto(e.getKey(), avgLoggedIn, avgAnonymous);
                })
                .sorted(Comparator.comparing(ActivityChartSplitPointDto::x))
                .toList();
    }

    private List<ActivityChartCombinedPointDto> buildCombinedPoints(
            List<ActivitySnapshot> snapshots, ActivityChartDensity density) {

        if (density == ActivityChartDensity.TEN_MINUTES) {
            return snapshots.stream()
                    .map(s -> new ActivityChartCombinedPointDto(
                            s.getTimestamp(),
                            s.getLoggedInActiveCount() + (double) s.getAnonymousActiveCount()))
                    .toList();
        }

        return snapshots.stream()
                .collect(Collectors.groupingBy(s -> toBucketKey(s.getTimestamp(), density)))
                .entrySet().stream()
                .map(e -> {
                    double avgTotal = e.getValue().stream()
                            .mapToInt(s -> s.getLoggedInActiveCount() + s.getAnonymousActiveCount())
                            .average().orElse(0.0);
                    return new ActivityChartCombinedPointDto(e.getKey(), avgTotal);
                })
                .sorted(Comparator.comparing(ActivityChartCombinedPointDto::x))
                .toList();
    }

    // --- Daily-solvers chart (Endpoint B) ---

    @Transactional(readOnly = true)
    public AdminChartResponse<?> getDailySolversChart(
            DailySolversChartDensity density,
            DailySolversChartPeriod period,
            GameMode gameMode,
            boolean combined
    ) {
        if (gameMode == GameMode.QUOTES_STAGE_1) {
            return new AdminChartResponse<>(List.of());
        }

        LocalDate today = LocalDate.now(WARSAW);
        LocalDate from = switch (period) {
            case LAST_WEEK      -> today.minusWeeks(1);
            case LAST_MONTH     -> today.minusMonths(1);
            case LAST_3_MONTHS  -> today.minusMonths(3);
            case LAST_6_MONTHS  -> today.minusMonths(6);
        };

        List<DailySolverStatistic> rows =
                dailySolverStatisticRepository.findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
                        gameMode, from, today);

        return combined
                ? new AdminChartResponse<>(buildDailySolversCombinedPoints(rows, density))
                : new AdminChartResponse<>(buildDailySolversSplitPoints(rows, density));
    }

    private LocalDate toWeekBucket(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<DailySolversChartSplitPointDto> buildDailySolversSplitPoints(
            List<DailySolverStatistic> rows, DailySolversChartDensity density) {

        if (density == DailySolversChartDensity.DAY) {
            return rows.stream()
                    .map(r -> new DailySolversChartSplitPointDto(
                            r.getPuzzleDate(),
                            r.getTotalSolvers(),
                            r.getTotalAnonymousSolvers()))
                    .toList();
        }

        return rows.stream()
                .collect(Collectors.groupingBy(r -> toWeekBucket(r.getPuzzleDate())))
                .entrySet().stream()
                .map(e -> {
                    List<DailySolverStatistic> bucket = e.getValue();
                    double avgLoggedIn  = bucket.stream().mapToInt(DailySolverStatistic::getTotalSolvers).average().orElse(0.0);
                    double avgAnonymous = bucket.stream().mapToInt(DailySolverStatistic::getTotalAnonymousSolvers).average().orElse(0.0);
                    return new DailySolversChartSplitPointDto(e.getKey(), avgLoggedIn, avgAnonymous);
                })
                .sorted(Comparator.comparing(DailySolversChartSplitPointDto::x))
                .toList();
    }

    private List<DailySolversChartCombinedPointDto> buildDailySolversCombinedPoints(
            List<DailySolverStatistic> rows, DailySolversChartDensity density) {

        if (density == DailySolversChartDensity.DAY) {
            return rows.stream()
                    .map(r -> new DailySolversChartCombinedPointDto(
                            r.getPuzzleDate(),
                            r.getTotalSolvers() + (double) r.getTotalAnonymousSolvers()))
                    .toList();
        }

        return rows.stream()
                .collect(Collectors.groupingBy(r -> toWeekBucket(r.getPuzzleDate())))
                .entrySet().stream()
                .map(e -> {
                    double avgTotal = e.getValue().stream()
                            .mapToInt(r -> r.getTotalSolvers() + r.getTotalAnonymousSolvers())
                            .average().orElse(0.0);
                    return new DailySolversChartCombinedPointDto(e.getKey(), avgTotal);
                })
                .sorted(Comparator.comparing(DailySolversChartCombinedPointDto::x))
                .toList();
    }
}
