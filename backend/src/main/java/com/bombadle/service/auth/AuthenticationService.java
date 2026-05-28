package com.bombadle.service.auth;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.exception.RegistrationValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final PlayerRepository repo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AnonymousMergeService mergeService;

    @Transactional
    public String register(RegisterRequest request, UUID anonymousSessionId, Boolean triggerMerge) {
        if (repo.existsByEmail(request.getEmail()) || repo.existsByLogin(request.getUsername())) {
            throw new RegistrationConflictException("Email or username already exists");
        }

        if (request.getPassword().length() < 8 || request.getPassword().length() > 24) {
            throw new RegistrationValidationException("Password must be between 8 and 24 characters");
        }

        if (request.getUsername().length() < 3 || request.getUsername().length() > 16) {
            throw new RegistrationValidationException("Username must be between 3 and 16 characters");
        }

        if (!request.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RegistrationValidationException("Invalid email format");
        }

        var player = Player.builder()
                .login(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .hasGuessedToday(false)
                .accountLocked(false)
                .build();

        repo.save(player);
        log.info("Registered new user: {}", player.getLogin());

        mergeService.handleAnonymousSessionMerge(player, anonymousSessionId, triggerMerge);

        return jwtService.generateJwtToken(player);
    }


    @Transactional
    public String authenticate(AuthenticationRequest request, UUID anonymousSessionId, Boolean triggerMerge) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            var user = repo.findByEmail(request.getEmail())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
            user.setLastLoginAt(Instant.now());
            repo.save(user);

             mergeService.handleAnonymousSessionMerge(user, anonymousSessionId, triggerMerge);

            return jwtService.generateJwtToken(user);
        }catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    public Boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    public Boolean existsByUsername(String username) {
        return repo.existsByLogin(username);
    }

}
