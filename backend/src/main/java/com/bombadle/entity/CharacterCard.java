package com.bombadle.entity;

import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Gender;
import com.bombadle.enums.Race;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Entity
@Table(name = "character_card")
@Getter
public class CharacterCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    @Enumerated(EnumType.STRING)
    private Race race;

    @Column
    private Boolean alive;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "character_affiliations",
            joinColumns = @JoinColumn(name = "character_id")
    )
    @Column(name = "affiliation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Affiliation> affiliations = new HashSet<>();

    @Column(name = "first_appearance_episode")
    private int firstAppearanceEpisode;

    @OneToMany(
            mappedBy = "characterCard",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Quote> quotes = new HashSet<>();

}
