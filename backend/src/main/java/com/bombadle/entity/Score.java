package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "score")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "score_time_stamp", nullable = false)
    private Instant scoreTimestamp;

    @Column(name = "number_of_tries", nullable = false)
    private int numberOfTries;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode")
    private GameMode gameMode;

    @Builder
    public Score(Player player, Instant scoreTimestamp, int numberOfTries, GameMode gameMode) {
        this.player = player;
        this.scoreTimestamp = scoreTimestamp;
        this.numberOfTries = numberOfTries;
        this.gameMode = gameMode;
    }
}
