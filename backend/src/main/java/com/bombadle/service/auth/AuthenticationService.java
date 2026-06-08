package com.bombadle.service.auth;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.exception.UnverifiedEmailException;
import com.bombadle.service.auth.email.EmailActionInitiator;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final PlayerService playerService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailActionInitiator emailActionInitiator;
    private final ApplicationConfigProperties.EmailConfig emailConfig;

    @Transactional
    public Player register(RegisterRequest requestData) {
        String normalizedEmail = requestData.email().toLowerCase();
        String normalizedUsername = requestData.username().toLowerCase();

        if (playerService.existsByEmail(normalizedEmail) || playerService.existsByLogin(normalizedUsername)) {
            throw new RegistrationConflictException("Email or username already exists");
        }

        var player = Player.builder()
                .displayName(requestData.username())
                .login(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(requestData.password()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .hasGuessedToday(false)
                .accountLocked(false)
                .emailVerified(emailConfig.autoActivateAccount())
                .build();

        Player savedPlayer = playerService.save(player);
        log.info("Registered new user: {}", savedPlayer.getLogin());

        if(!emailConfig.autoActivateAccount())
            emailActionInitiator.initiateAccountActivation(savedPlayer);

        return savedPlayer;
    }

    @Transactional
    public Player authenticate(AuthenticationRequest requestData) {
        String normalizedEmail = requestData.email().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            requestData.password()
                    )
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        var player = playerService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!player.getEmailVerified()) {
            throw new UnverifiedEmailException("Account isn't verified", player.getEmail());
        }

        player.setLastActiveAt(Instant.now());
        return playerService.save(player);
    }

    public Boolean existsByEmail(String email) {
        return playerService.existsByEmail(email);
    }

    public Boolean existsByUsername(String username) {
        return playerService.existsByLogin(username);
    }
}