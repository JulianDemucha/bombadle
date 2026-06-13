package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "anonymous_guess_list_id")
    private AnonymousGuessList guessList;

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
        return completedModesToday.contains(mode);
    }

    public void markModeAsCompleted(GameMode mode) {
        this.completedModesToday.add(mode);
    }

    public void addScoreTimestamp(GameMode mode, Instant timestamp) {
        this.scoreTimestamps.put(mode, timestamp);
    }

    public void resetDailyProgress() {
        this.completedModesToday.clear();
        this.scoreTimestamps.clear();
    }

    public AnonymousSession(AnonymousGuessList guessList) {
        this.guessList = guessList;
        this.completedModesToday = new HashSet<>();
        this.scoreTimestamps = new HashMap<>();
    }

    public static AnonymousSession createEmptySession() {
        return AnonymousSession.builder()
                .guessList(AnonymousGuessList.builder()
                        .guesses(new HashMap<>())
                        .build())
                .lastActiveAt(Instant.now())
                .completedModesToday(new HashSet<>())
                .scoreTimestamps(new HashMap<>())
                .build();
    }
}