package com.bombadle.service.auth;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.RegistrationConflictException;
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

    @Transactional
    public Player register(RegisterRequest requestData) {
        String normalizedEmail = requestData.getEmail().toLowerCase();
        String normalizedUsername = requestData.getUsername().toLowerCase();
        
        if (playerService.existsByEmail(normalizedEmail) || playerService.existsByLogin(normalizedUsername)) {
            throw new RegistrationConflictException("Email or username already exists");
        }

        var player = Player.builder()
                .displayName(requestData.getUsername())
                .login(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(requestData.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .hasGuessedToday(false)
                .accountLocked(false)
                .build();

        log.info("Registered new user: {}", player.getLogin());
        return playerService.save(player);
    }


    @Transactional
    public Player authenticate(AuthenticationRequest requestData) {
        try {
            String normalizedEmail = requestData.getEmail().toLowerCase();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            requestData.getPassword()
                    )
            );
            var player = playerService.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
            player.setLastActiveAt(Instant.now());
            return playerService.save(player);
        }catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    public Boolean existsByEmail(String email) {
        return playerService.existsByEmail(email);
    }

    public Boolean existsByUsername(String username) {
        return playerService.existsByLogin(username);
    }

}
