package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AccountLockedFilter extends OncePerRequestFilter {

    private static final List<RequestMatcher> ALLOWED_WHEN_LOCKED = List.of(
            new AntPathRequestMatcher("/api/auth/**"),
            new AntPathRequestMatcher("/api/players/me", "GET"),
            new AntPathRequestMatcher("/api/leaderboard/**")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedWhenLocked(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAccountLocked(authentication.getPrincipal())) {
            response.setStatus(423);
            response.setContentType("application/json");
            response.getWriter().write("{\"statusCode\":423,\"error\":\"Account Locked\",\"message\":\"Account is locked\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWhenLocked(HttpServletRequest request) {
        return ALLOWED_WHEN_LOCKED.stream().anyMatch(matcher -> matcher.matches(request));
    }

    private boolean isAccountLocked(Object principal) {
        if (principal instanceof PlayerPrincipal playerPrincipal) {
            return playerPrincipal.isAccountLocked();
        }
        if (principal instanceof CustomOAuth2PlayerUser oauth2User) {
            return Boolean.TRUE.equals(oauth2User.getPlayer().getAccountLocked());
        }
        return false;
    }
}
