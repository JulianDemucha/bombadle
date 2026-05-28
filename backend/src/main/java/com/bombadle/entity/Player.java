package com.bombadle.entity;

import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "player")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Player{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "today_score_id", unique = true)
    private Score todayScore;

    @Column(name = "avatar_image", nullable = false)
    @Enumerated(EnumType.STRING)
    private AvatarImage avatarImage;

    @Column(name = "total_guesses", nullable = false)
    private int totalSuccessfulGuesses;

    @Column(name = "has_guessed_today", nullable = false)
    private Boolean hasGuessedToday;

    @Builder.Default
    @Column(name = "account_locked", nullable = false)
    private Boolean accountLocked = false;

    @Column(name = "marked_for_deletion_at")
    private Instant markedForDeletionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private PlayerAuthProvider authProvider;
}
