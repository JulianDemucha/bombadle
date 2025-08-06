package com.bombadle.security.auth.dto;

import lombok.Builder;


//todo -> AuthenticationService ?
@Builder
public record AuthenticatedPlayerDto(
        Long id,
        String username,
        String password,
        String email,
        String role,
        String avatarImage,
        String createdAt,
        boolean hasGuessedToday,
        String token
) {
}
