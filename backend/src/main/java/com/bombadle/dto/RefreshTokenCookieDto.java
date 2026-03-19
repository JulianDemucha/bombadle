package com.bombadle.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RefreshTokenCookieDto {
    private String refreshToken;
    private Instant expiresAt;
    private String jwt;
}
