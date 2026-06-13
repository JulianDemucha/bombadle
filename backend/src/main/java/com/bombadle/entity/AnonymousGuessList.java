package com.bombadle.entity;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousGuessList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<GameMode, List<GuessAttempt>> guesses = new HashMap<>();

    public void addGuess(GameMode mode, GuessAttempt attempt) {
        this.guesses.computeIfAbsent(mode, k -> new ArrayList<>()).add(attempt);
    }
}
