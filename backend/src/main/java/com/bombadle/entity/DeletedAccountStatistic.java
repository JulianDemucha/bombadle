package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Snapshot of a player's aggregate statistics, intended to be captured when the player is
 * cascade-deleted (mirroring {@link DeletedAccount}).
 * <p>
 * Entity only for now: the snapshotting logic and the coupling that removes this row together
 * with its {@link DeletedAccount} are intentionally deferred to a later stage.
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

    @Column(name = "average_leaderboard_percentile")
    private Double averageLeaderboardPercentile;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
