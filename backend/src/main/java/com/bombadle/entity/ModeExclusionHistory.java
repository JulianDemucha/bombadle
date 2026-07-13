package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mode_exclusion_history", uniqueConstraints = {
        @UniqueConstraint(name = "uc_mode_exclusion_history_game_mode", columnNames = {"game_mode"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModeExclusionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "excluded_ids")
    private Set<Long> excludedIds = new HashSet<>();

    public void addExcludedId(Long id) {
        // Replacing the field with a new instance (not mutating in-place) is required so
        // Hibernate's ImmutableMutabilityPlan dirty checker sees a changed reference and
        // emits the UPDATE for the jsonb excluded_ids column. In-place add() leaves snapshot == current.
        Set<Long> updated = this.excludedIds == null ? new HashSet<>() : new HashSet<>(this.excludedIds);
        updated.add(id);
        this.excludedIds = updated;
    }

    public void resetExcludedIdsTo(Long onlyId) {
        Set<Long> reset = new HashSet<>();
        if (onlyId != null) {
            reset.add(onlyId);
        }
        this.excludedIds = reset;
    }
}
