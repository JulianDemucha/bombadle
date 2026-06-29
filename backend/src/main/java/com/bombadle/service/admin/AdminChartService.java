package com.bombadle.service.admin;

import com.bombadle.dto.ActivityChartCombinedPointDto;
import com.bombadle.dto.ActivityChartSplitPointDto;
import com.bombadle.dto.AdminChartResponse;
import com.bombadle.entity.ActivitySnapshot;
import com.bombadle.enums.ActivityChartDensity;
import com.bombadle.enums.ActivityChartPeriod;
import com.bombadle.repository.ActivitySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminChartService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ActivitySnapshotRepository activitySnapshotRepository;

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
}
