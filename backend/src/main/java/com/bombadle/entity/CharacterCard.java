package com.bombadle.entity;

import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Race;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "character_card")
public class CharacterCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private Boolean alive;

    @Column
    @Enumerated(EnumType.STRING)
    private Race race;

    @Column(name = "first_appearance_episode")
    private int firstAppearanceEpisode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "character_affiliations",
            joinColumns = @JoinColumn(name = "character_id")
    )
    @Column(name = "affiliation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Affiliation> affiliations = new HashSet<>();

    @OneToMany(
            mappedBy = "characterCard",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Quote> quotes = new HashSet<>();

}
