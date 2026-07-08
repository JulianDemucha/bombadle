package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "anonymous_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Builder.Default
    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AnonymousGuessList> guessLists = new ArrayList<>();

    @Builder.Default
    @Column(name = "lastActiveAt", nullable = false)
    private Instant lastActiveAt = Instant.now();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Set<GameMode> completedModesToday = new HashSet<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "score_timestamps")
    private Map<GameMode, Instant> scoreTimestamps = new HashMap<>();

    public boolean hasGuessedToday(GameMode mode) {
        if (completedModesToday == null) return false;
        return completedModesToday.contains(mode);
    }

    public void markModeAsCompleted(GameMode mode) {
        // Replacing the field with a new instance (not mutating in-place) is required so
        // Hibernate's ImmutableMutabilityPlan dirty checker sees a changed reference and
        // emits the UPDATE for completed_modes_today. In-place add() leaves snapshot == current.
        Set<GameMode> updated = this.completedModesToday == null ? new HashSet<>() : new HashSet<>(this.completedModesToday);
        updated.add(mode);
        this.completedModesToday = updated;
    }

    public void addScoreTimestamp(GameMode mode, Instant timestamp) {
        // Same reason as markModeAsCompleted: replace, don't put() in-place, to force dirty detection.
        Map<GameMode, Instant> updated = this.scoreTimestamps == null ? new HashMap<>() : new HashMap<>(this.scoreTimestamps);
        updated.put(mode, timestamp);
        this.scoreTimestamps = updated;
    }

    public void resetDailyProgress() {
        // Same reason as markModeAsCompleted: replace, don't clear(), to force dirty detection.
        this.completedModesToday = new HashSet<>();
        this.scoreTimestamps = new HashMap<>();
        if (this.guessLists != null) this.guessLists.clear();
    }

    public void addGuessList(AnonymousGuessList guessList) {
        if (this.guessLists == null) {
            this.guessLists = new ArrayList<>();
        }
        this.guessLists.add(guessList);
        guessList.setSession(this);
    }

    public Optional<AnonymousGuessList> getGuessListForMode(GameMode gameMode) {
        if (this.guessLists == null) return Optional.empty();

        return this.guessLists.stream()
                .filter(list -> list.getGameMode() == gameMode)
                .findFirst();
    }

    public static AnonymousSession createEmptySession() {
        return AnonymousSession.builder()
                .lastActiveAt(Instant.now())
                .completedModesToday(new HashSet<>())
                .scoreTimestamps(new HashMap<>())
                .guessLists(new ArrayList<>())
                .build();
    }
}