package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_character_id")
    private CharacterCard currentCharacter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "previous_character_id")
    private CharacterCard previousCharacter;

}