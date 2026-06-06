package com.bombadle.entity;

import com.bombadle.dto.GuessAttempt;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class GuessList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<GuessAttempt> guesses;

    public GuessList(Player player) {
        this.player = player;
        this.guesses = new ArrayList<>();
    }
}
