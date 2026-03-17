package com.bombadle.entity;

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

    @OneToOne(mappedBy = "todayScore", fetch = FetchType.LAZY)
    private Player player;

    @Column(name = "score_time_stamp", nullable = false)
    private Instant scoreTimestamp;

    @Column(name = "number_of_tries", nullable = false)
    private int numberOfTries;

    @Builder
    public Score(Player player, Instant scoreTimestamp, int numberOfTries) {
        this.player = player;
        this.scoreTimestamp = scoreTimestamp;
        this.numberOfTries = numberOfTries;
    }
}
