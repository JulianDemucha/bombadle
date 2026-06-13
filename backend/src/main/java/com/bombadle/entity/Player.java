package com.bombadle.entity;

import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Data
@Entity
@Table(name = "player")
@Getter
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(name = "display_name", length = 16)
    private String displayName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "lastActiveAt", nullable = false)
    private Instant lastActiveAt = Instant.now();

    @OneToMany(
            mappedBy = "player",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @MapKey(name = "gameMode")
    private Map<GameMode, Score> todayScores;

    @Column(name = "avatar_image", nullable = false)
    @Enumerated(EnumType.STRING)
    private AvatarImage avatarImage;

    @Column(name = "total_guesses", nullable = false)
    private int totalSuccessfulGuesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<GameMode> completedModesToday;

    @Column(name = "account_locked", nullable = false)
    private Boolean accountLocked = false;

    @Column(name = "marked_for_deletion_at")
    private Instant markedForDeletionAt;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_email_sent_at")
    private Instant lastEmailSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private PlayerAuthProvider authProvider;

    @Builder
    protected Player(Long id, String login, String displayName, String email, String passwordHash, Role role,
                     Instant createdAt, Instant lastActiveAt, Map<GameMode, Score> todayScores,
                     AvatarImage avatarImage, int totalSuccessfulGuesses, Set<GameMode> completedModesToday,
                     Boolean accountLocked, Instant markedForDeletionAt, Boolean emailVerified,
                     Instant lastEmailSentAt, PlayerAuthProvider authProvider) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt != null ? lastActiveAt : Instant.now();
        this.todayScores = todayScores == null ? new HashMap<>() : todayScores;
        this.avatarImage = avatarImage;
        this.totalSuccessfulGuesses = totalSuccessfulGuesses;
        this.completedModesToday = completedModesToday == null ? new HashSet<>() : completedModesToday;
        this.accountLocked = accountLocked != null ? accountLocked : false;
        this.emailVerified = emailVerified != null ? emailVerified : false;
        this.markedForDeletionAt = markedForDeletionAt;
        this.lastEmailSentAt = lastEmailSentAt;
        this.authProvider = authProvider;
    }

    protected Player() {
    }

    public boolean hasGuessedToday(GameMode mode) {
        return completedModesToday.contains(mode);
    }

    private void markModeAsCompleted(GameMode mode) {
        this.completedModesToday.add(mode);
    }

    public void resetDailyProgress() {
        this.completedModesToday.clear();
        this.todayScores.clear();
    }

    public Optional<Score> getTodayScore(GameMode mode) {
        return Optional.ofNullable(todayScores.get(mode));
    }

    public void addTodayScore(GameMode gameMode, Score score) {
        markModeAsCompleted(gameMode);
        setTotalSuccessfulGuesses(
                getTotalSuccessfulGuesses() + 1
        );

        score.setPlayer(this);
        if (!score.getGameMode().equals(gameMode)) {
            throw new IllegalArgumentException(
                    String.format("Score game mode (%s) does not match the provided mode (%s)", score.getGameMode(), gameMode)
            );
        }

        this.todayScores.put(gameMode, score);
    }

    @PostLoad
    private void ensureCollectionsInitialized() {
        if (this.completedModesToday == null) {
            this.completedModesToday = new HashSet<>();
        }
        if (this.todayScores == null) {
            this.todayScores = new HashMap<>();
        }
    }
}