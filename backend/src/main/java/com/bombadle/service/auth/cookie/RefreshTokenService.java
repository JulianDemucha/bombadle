package com.bombadle.service.auth.cookie;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.RefreshToken;
import com.bombadle.repository.RefreshTokenRepository;

import com.bombadle.service.auth.JwtService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PlayerService playerService;
    private final JwtService jwtService;
    private final ApplicationConfigProperties.JwtConfig jwtConfig;

    public RefreshTokenCookieDto createRefreshToken(String email) {
        String token = UUID.randomUUID().toString();
        String hashedToken = DigestUtils.sha256Hex(token);
        Instant expiresAt = Instant.now().plusSeconds(jwtConfig.refreshExpirationSeconds()); //1h
        Player player = playerService.findByEmail(email.toLowerCase()).orElseThrow(
                () -> new UsernameNotFoundException("User not found: " + email)
        );


        RefreshToken refreshToken = RefreshToken.builder()
                .player(player)
                .tokenHash(hashedToken)
                .expiresAt(expiresAt)
                .revoked(false)
                .revokedAt(null)
                .build();

        refreshTokenRepository.save(refreshToken);

        return RefreshTokenCookieDto.builder()
                .refreshToken(token)
                .expiresAt(expiresAt)
                .jwt(jwtService.generateJwtToken(new PlayerPrincipal(player)))
                .build();
    }

    public Optional<RefreshToken> findByPlayerId(Long playerId) {
        return refreshTokenRepository.findByPlayerId(playerId);
    }

    public void manualDelete(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }


    public RefreshToken getRefreshTokenByToken(String token){
        return refreshTokenRepository.findByTokenHash(
                DigestUtils.sha256Hex(token)
        ).orElseThrow();
    }

    public void revokeRefreshToken(String refreshToken) {
        RefreshToken RefreshToken = getRefreshTokenByToken(refreshToken);
        RefreshToken.setRevoked(true);
        RefreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(RefreshToken);
    }


    @Transactional
    public RefreshTokenCookieDto createRefreshTokenAndRevokeOld(String oldToken) {
        Player player = getRefreshTokenByToken(oldToken).getPlayer();
        String newToken = UUID.randomUUID().toString();
        String hashedNewToken = DigestUtils.sha256Hex(newToken);
        Instant expiresAt = Instant.now().plusSeconds(60 * 60); //1h

        RefreshToken newRefreshToken = RefreshToken.builder()
                .player(player)
                .tokenHash(hashedNewToken)
                .expiresAt(expiresAt)
                .revoked(false)
                .revokedAt(null)
                .build();

        refreshTokenRepository.save(newRefreshToken);
        revokeRefreshToken(oldToken);

        return RefreshTokenCookieDto.builder()
                .refreshToken(newToken)
                .jwt(jwtService.generateJwtToken(new PlayerPrincipal(player)))
                .build();
    }

    @Transactional
    public void deleteAllByPlayerId(long playerId) {
        refreshTokenRepository.deleteByPlayerId(playerId);
    }


    @Transactional
    public int deleteRevokedRefreshTokens(int secondsBeforeCutoff) {
        return refreshTokenRepository.deleteRevokedBeforeCutoff(Instant.now().minusSeconds(secondsBeforeCutoff));
    }


}
