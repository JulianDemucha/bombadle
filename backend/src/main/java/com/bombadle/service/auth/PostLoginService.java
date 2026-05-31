package com.bombadle.service.auth;


import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.entity.Player;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostLoginService {

    private final AuthCookiesService authCookiesService;
    private final RefreshTokenService refreshTokenService;
    private final CsrfCookieService csrfCookieService;
    private final AnonymousMergeService anonymousMergeService;
    private final CookieService cookieService;

    @Transactional
    public void processSuccessfulLogin(HttpServletRequest request, HttpServletResponse response, Player player) {
        UUID anonSessionId = cookieService.getCookieValue(
                request,
                "ANON_SESSION_ID",
                UUID::fromString
        ).orElse(null);

        Boolean triggerMerge = cookieService.getCookieValue(
                request,
                "TRIGGER_MERGE",
                Boolean::valueOf
        ).orElse(false);

        anonymousMergeService.handleAnonymousSessionMerge(
                player,
                anonSessionId,
                triggerMerge
        );

        RefreshTokenCookieDto refreshData = refreshTokenService.createRefreshToken(player.getEmail());

        authCookiesService.setAuthCookies(
                refreshData.getJwt(),
                refreshData.getRefreshToken(),
                response
        );
        csrfCookieService.ensureCsrfCookie(request, response);

        if (anonSessionId != null) {
            cookieService.deleteCookieFromResponse(response, "ANON_SESSION_ID");
        }
        if (triggerMerge) {
            cookieService.deleteCookieFromResponse(response, "TRIGGER_MERGE");
        }

    }
}