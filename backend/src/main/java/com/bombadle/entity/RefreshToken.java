package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 64) //sha-256
    private String tokenHash;

    private Instant expiresAt;

    private boolean revoked;

    private Instant revokedAt;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;
}
