package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "current_card_state")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
// SINGLETON
public class CurrentCardState {

    @Id
    private Integer id = 1;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "current_card_mapping",
            joinColumns = @JoinColumn(name = "state_id"),
            inverseJoinColumns = @JoinColumn(name = "character_id")
    )
    @MapKeyColumn(name = "game_mode")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<GameMode, CharacterCard> currentCards = new HashMap<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "previous_card_mapping",
            joinColumns = @JoinColumn(name = "state_id"),
            inverseJoinColumns = @JoinColumn(name = "character_id")
    )
    @MapKeyColumn(name = "game_mode")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<GameMode, CharacterCard> previousCards = new HashMap<>();
}