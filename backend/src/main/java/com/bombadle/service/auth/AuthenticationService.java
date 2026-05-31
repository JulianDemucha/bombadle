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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final PlayerRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PostLoginService postLoginService;

    @Transactional
    public void register(RegisterRequest requestData, HttpServletRequest request, HttpServletResponse response) {
        if (repo.existsByEmail(requestData.getEmail()) || repo.existsByLogin(requestData.getUsername())) {
            throw new RegistrationConflictException("Email or username already exists");
        }

        if (requestData.getPassword().length() < 8 || requestData.getPassword().length() > 24) {
            throw new RegistrationValidationException("Password must be between 8 and 24 characters");
        }

        if (requestData.getUsername().length() < 3 || requestData.getUsername().length() > 16) {
            throw new RegistrationValidationException("Username must be between 3 and 16 characters");
        }

        if (!requestData.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RegistrationValidationException("Invalid email format");
        }

        var player = Player.builder()
                .login(requestData.getUsername())
                .email(requestData.getEmail())
                .passwordHash(passwordEncoder.encode(requestData.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .hasGuessedToday(false)
                .accountLocked(false)
                .build();

        repo.save(player);
        log.info("Registered new user: {}", player.getLogin());

        postLoginService.processSuccessfulLogin(request, response, player);
    }


    @Transactional
    public void authenticate(AuthenticationRequest requestData, HttpServletRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestData.getEmail(),
                            requestData.getPassword()
                    )
            );
            var player = repo.findByEmail(requestData.getEmail())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
            player.setLastActiveAt(Instant.now());
            repo.save(player);

            postLoginService.processSuccessfulLogin(request, response, player);
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
