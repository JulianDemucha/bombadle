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

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "current_superstreak", nullable = false)
    private int currentSuperstreak;

    @Column(name = "longest_superstreak", nullable = false)
    private int longestSuperstreak;

    public static final Set<GameMode> ALL_GAME_MODES = Set.copyOf(EnumSet.allOf(GameMode.class));

    @Builder
    protected Player(Long id, String login, String displayName, String email, String passwordHash, Role role,
                     Instant createdAt, Instant lastActiveAt, Map<GameMode, Score> todayScores,
                     AvatarImage avatarImage, int totalSuccessfulGuesses, Set<GameMode> completedModesToday,
                     Boolean accountLocked, Instant markedForDeletionAt, Boolean emailVerified,
                     Instant lastEmailSentAt, PlayerAuthProvider authProvider,
                     int currentStreak, int longestStreak, int currentSuperstreak, int longestSuperstreak) {
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
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.currentSuperstreak = currentSuperstreak;
        this.longestSuperstreak = longestSuperstreak;
    }

    protected Player() {
    }

    public boolean hasGuessedToday(GameMode mode) {
        return completedModesToday.contains(mode);
    }

    private void markModeAsCompleted(GameMode mode) {
        // Replacing the field with a new instance (not mutating in-place) is required so
        // Hibernate's ImmutableMutabilityPlan dirty checker sees a changed reference and
        // emits the UPDATE for completed_modes_today. In-place add() leaves snapshot == current.
        Set<GameMode> updated = new HashSet<>(this.completedModesToday);
        updated.add(mode);
        this.completedModesToday = updated;
    }

    public void resetDailyProgress() {
        // Same reason as markModeAsCompleted: replace, don't clear(), to force dirty detection.
        this.completedModesToday = new HashSet<>();
        this.todayScores.clear();
    }

    /**
     * Zeroes streak counters at the daily 07:00 boundary for players who did not meet the
     * daily threshold. Increments are applied in real-time by {@link #addTodayScore} as each
     * mode is solved, so this method must never increment — only zero out.
     *
     * @param playedToday       true if the player completed at least one mode (streak safe)
     * @param completedAllModes true if the player completed every mode (superstreak safe)
     */
    public void resetStreaksIfThresholdsNotMet(boolean playedToday, boolean completedAllModes) {
        if (!playedToday) {
            currentStreak = 0;
        }
        if (!completedAllModes) {
            currentSuperstreak = 0;
        }
    }

    public Optional<Score> getTodayScore(GameMode mode) {
        return Optional.ofNullable(todayScores.get(mode));
    }

    public void addTodayScore(GameMode gameMode, Score score) {
        if (!score.getGameMode().equals(gameMode)) {
            throw new IllegalArgumentException(
                    String.format("Score game mode (%s) does not match the provided mode (%s)", score.getGameMode(), gameMode)
            );
        }

        boolean wasFirstSolveToday = completedModesToday.isEmpty();
        boolean wasAllModesAlreadyDone = completedModesToday.containsAll(ALL_GAME_MODES);

        markModeAsCompleted(gameMode);
        setTotalSuccessfulGuesses(getTotalSuccessfulGuesses() + 1);
        score.setPlayer(this);
        this.todayScores.put(gameMode, score);

        if (wasFirstSolveToday) {
            currentStreak++;
            longestStreak = Math.max(longestStreak, currentStreak);
        }
        if (!wasAllModesAlreadyDone && completedModesToday.containsAll(ALL_GAME_MODES)) {
            currentSuperstreak++;
            longestSuperstreak = Math.max(longestSuperstreak, currentSuperstreak);
        }
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