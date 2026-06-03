package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
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

    @Column(name = "lastActiveAt", nullable = false)
    private Instant lastActiveAt = Instant.now();

    private boolean hasGuessedToday = false;

    @Column(nullable = true)
    private Instant scoreTimestamp;

    public boolean hasGuessedToday(){
        return hasGuessedToday;
    }

    public AnonymousSession(AnonymousGuessList guessList) {
        this.guessList = guessList;
    }
}
