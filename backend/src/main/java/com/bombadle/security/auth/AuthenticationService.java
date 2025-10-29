package com.bombadle.security.auth;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.mapper.PlayerMapper;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.security.auth.dto.AuthenticationRequest;
import com.bombadle.security.auth.dto.RegisterRequest;
import com.bombadle.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final PlayerRepository repo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PlayerMapper playerMapper;

    public String register(RegisterRequest request) {
        if (repo.existsByEmail(request.getEmail()) || repo.existsByLogin(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email or username already exists");
        }

        if (request.getPassword().length() < 8 || request.getPassword().length() > 24) {
            throw new ResponseStatusException((HttpStatus.CONFLICT),
                    "Password must be between 8 and 24 characters");
        }

        if (request.getUsername().length() < 3 || request.getUsername().length() > 16) {
            throw new ResponseStatusException((HttpStatus.CONFLICT),
                    "Username must be between 3 and 16 characters");
        }

        if (!request.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException((HttpStatus.CONFLICT),
                    "Invalid email format");
        }

        var user = Player.builder()
                .login(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .authProvider(PlayerAuthProvider.LOCAL)
                .hasGuessedToday(false)
                .build();

        repo.save(user);
        log.info("Registered new user: {}", user.getLogin());

        return jwtService.generateJwtToken(user);
    }


    public String authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = repo.findByLogin(request.getUsername())
                .orElseThrow();
        user.setLastLoginAt(Instant.now());
        repo.save(user);
        return jwtService.generateJwtToken(user);
    }

    public Boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    public Boolean existsByUsername(String username) {
        return repo.existsByLogin(username);
    }

    public ResponseEntity<PlayerDto> getAuthenticatedPlayer(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Player player = repo.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Użytkownik z tokenu nie został znaleziony w bazie: " + userDetails.getUsername()));
        return ResponseEntity.ok(playerMapper.toDto(player));
    }
}
