package com.bombadle.entity;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "anonymous_guess_list")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousGuessList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anonymous_session_id", nullable = false)
    private AnonymousSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<GuessAttempt> guesses = new ArrayList<>();

    public void addGuess(GuessAttempt attempt) {
        // Replacing the field with a new instance (not mutating in-place) is required so
        // Hibernate's ImmutableMutabilityPlan dirty checker sees a changed reference and
        // emits the UPDATE for the jsonb guesses column. In-place add() leaves snapshot == current.
        List<GuessAttempt> updated = this.guesses == null ? new ArrayList<>() : new ArrayList<>(this.guesses);
        updated.add(attempt);
        this.guesses = updated;
    }
}