package com.bombadle.entity;

import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "deleted_account")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeletedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_player_id", nullable = false)
    private Long originalPlayerId;

    @Column(nullable = false)
    private String login;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "total_guesses", nullable = false)
    private int totalSuccessfulGuesses;

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_image", nullable = false)
    private AvatarImage avatarImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private PlayerAuthProvider authProvider;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(name = "deleted_by_actor_id")
    private Long deletedByActorId;
}

