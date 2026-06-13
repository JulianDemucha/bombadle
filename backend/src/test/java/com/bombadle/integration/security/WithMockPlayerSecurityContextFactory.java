package com.bombadle.integration.security;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.entity.Player;
import com.bombadle.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;

public class WithMockPlayerSecurityContextFactory implements WithSecurityContextFactory<WithMockPlayer> {

    @Override
    public SecurityContext createSecurityContext(WithMockPlayer annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Mock player entity setup
        Player mockPlayer = Player.builder()
                .id(1L)
                .email(annotation.username())
                .login(annotation.username().split("@")[0])
                .passwordHash("sigmasigmaboy")
                .role(Role.valueOf(annotation.role()))
                .accountLocked(annotation.accountLocked())
                .totalSuccessfulGuesses(0)
                .createdAt(Instant.now())
                .lastActiveAt(Instant.now())
                .build();

        PlayerPrincipal principal = new PlayerPrincipal(mockPlayer);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}