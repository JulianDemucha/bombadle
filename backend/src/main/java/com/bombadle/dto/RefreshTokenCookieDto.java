package com.bombadle.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenCookieDto {
    private String refreshToken;
    private String expiresAt;
    private String jwt;
}
