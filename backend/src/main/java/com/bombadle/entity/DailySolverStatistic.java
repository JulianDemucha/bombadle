package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Per-day, per-mode aggregate of how many players solved a puzzle, written once at the 07:00 daily
 * reset for the puzzle day that is closing. One row per ranked mode per puzzle day.
 * <p>
 * {@code totalSolvers} is the logged-in solver count (equal to the leaderboard participant count),
 * used as the denominator when computing a player's daily leaderboard percentile on read.
 * {@code totalAnonymousSolvers} is stored for future admin reporting only and is not used in the
 * percentile.
 */
@Entity
@Table(
        name = "daily_solver_statistic",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_solver_statistic_mode_date",
                columnNames = {"game_mode", "puzzle_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class DailySolverStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    @Column(name = "puzzle_date", nullable = false)
    private LocalDate puzzleDate;

    @Column(name = "total_solvers", nullable = false)
    private int totalSolvers;

    @Column(name = "total_anonymous_solvers", nullable = false)
    private int totalAnonymousSolvers;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Builder
    public DailySolverStatistic(GameMode gameMode, LocalDate puzzleDate, int totalSolvers,
                                int totalAnonymousSolvers, Instant capturedAt) {
        this.gameMode = gameMode;
        this.puzzleDate = puzzleDate;
        this.totalSolvers = totalSolvers;
        this.totalAnonymousSolvers = totalAnonymousSolvers;
        this.capturedAt = capturedAt;
    }
}
