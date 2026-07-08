package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "player_daily_statistic",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_daily_statistic_player_mode_date",
                columnNames = {"player_id", "game_mode", "puzzle_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class PlayerDailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    @Column(name = "puzzle_date", nullable = false)
    private LocalDate puzzleDate;

    @Column(name = "solved_at", nullable = false)
    private Instant solvedAt;

    @Column(name = "number_of_tries", nullable = false)
    private int numberOfTries;

    @Column(name = "leaderboard_position", nullable = false)
    private int leaderboardPosition;

    @Column(name = "total_participants", nullable = false)
    private int totalParticipants;

    @Builder
    public PlayerDailyStatistic(Player player, GameMode gameMode, LocalDate puzzleDate, Instant solvedAt,
                                int numberOfTries, int leaderboardPosition, int totalParticipants) {
        this.player = player;
        this.gameMode = gameMode;
        this.puzzleDate = puzzleDate;
        this.solvedAt = solvedAt;
        this.numberOfTries = numberOfTries;
        this.leaderboardPosition = leaderboardPosition;
        this.totalParticipants = totalParticipants;
    }
}
