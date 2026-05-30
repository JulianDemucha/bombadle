package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import com.bombadle.service.stats.ActivityTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivityTrackingFilter extends OncePerRequestFilter {

    private final ActivityTrackingService activityTrackingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            if(authentication.getPrincipal() instanceof PlayerPrincipal principal) {
                activityTrackingService.markPlayerActive(principal.getPlayerId());

            } else if(authentication.getPrincipal() instanceof CustomOAuth2PlayerUser principal) {
                activityTrackingService.markPlayerActive(principal.getPlayer().getId());

            } else{
                checkAnonymousCookie(request);
            }

        } else {
            checkAnonymousCookie(request);
        }

        filterChain.doFilter(request, response);
    }

    private void checkAnonymousCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> anonSessionIdOpt = Arrays.stream(request.getCookies())
                    .filter(c -> "ANON_SESSION_ID".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst();

            anonSessionIdOpt.ifPresent(idStr -> {
                try {
                    UUID sessionId = UUID.fromString(idStr);
                    activityTrackingService.markAnonymousActive(sessionId);
                } catch (IllegalArgumentException ignored) {
                }
            });
        }
    }
}