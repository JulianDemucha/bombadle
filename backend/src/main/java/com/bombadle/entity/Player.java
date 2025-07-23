package com.bombadle.entity;

import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "today_score_id", unique = true)
    private Score todayScore;

    @Column(name = "avatar_image")
    @Enumerated(EnumType.STRING)
    private AvatarImage avatarImage;

    @Column(name = "total_guesses")
    private int totalGuesses;

    @Column(name = "has_guessed_today")
    private Boolean hasGuessedToday;

}
