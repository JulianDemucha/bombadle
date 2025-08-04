package com.bombadle.security.auth;

import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Role;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.security.auth.dto.AuthenticationRequest;
import com.bombadle.security.auth.dto.AuthenticationResponse;
import com.bombadle.security.auth.dto.RegisterRequest;
import com.bombadle.security.jwt.JwtService;
import com.bombadle.service.CharacterCardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PlayerRepository repo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationResponse register(RegisterRequest request) {
        var user = Player.builder()
                .login(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .lastLoginAt(null)
                .avatarImage(AvatarImage.AVATAR_DEFAULT)
                .build();

        repo.save(user);
        var jwtToken = jwtService.generateJwtToken(user);
        log.info("Registered new user: {}", user.getLogin());

        //todo implement AuthenticatedPlayerDto and its Mapper, dont return entity
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = repo.findByLogin(request.getUsername())
                .orElseThrow();
        var jwtToken = jwtService.generateJwtToken(user);

        //todo implement AuthenticatedPlayerDto and its Mapper, dont return entity
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
