package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "score")
@NoArgsConstructor
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

}
