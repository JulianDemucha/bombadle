package com.bombadle.entity;

import com.bombadle.dto.DailyStatisticSnapshot;
import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of a player's statistics, captured when the player is cascade-deleted (mirroring
 * {@link DeletedAccount}). {@link #dailyStatisticsSnapshot} preserves the full per-day history
 * so it can be replayed back into {@code PlayerDailyStatistic} rows on account recovery, since
 * those rows are otherwise wiped by the cascade.
 */
@Entity
@Table(name = "deleted_account_statistic")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeletedAccountStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_account_id", nullable = false)
    private Long deletedAccountId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "current_superstreak", nullable = false)
    private int currentSuperstreak;

    @Column(name = "longest_superstreak", nullable = false)
    private int longestSuperstreak;

    @Column(name = "total_guesses", nullable = false)
    private int totalSuccessfulGuesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "guesses_by_mode", columnDefinition = "jsonb")
    private Map<GameMode, Integer> guessesByMode;

    @Column(name = "total_top3_finishes", nullable = false)
    private int totalTop3Finishes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "daily_statistics_snapshot", columnDefinition = "jsonb")
    private List<DailyStatisticSnapshot> dailyStatisticsSnapshot;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
