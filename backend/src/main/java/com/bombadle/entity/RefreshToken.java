package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Player player;
}
