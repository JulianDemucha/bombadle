package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Feedback(String title, String description, Long playerId, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.playerId = playerId;
        this.createdAt = createdAt;
    }
}
