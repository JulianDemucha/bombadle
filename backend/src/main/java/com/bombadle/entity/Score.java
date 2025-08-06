package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "score")
@NoArgsConstructor
@Getter
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "todayScore", fetch = FetchType.LAZY)
    private Player player;

    @Column(name = "score_time_stamp", nullable = false)
    private LocalDateTime scoreTimestamp;

    @Column(name = "number_of_tries", nullable = false)
    private int numberOfTries;

}
