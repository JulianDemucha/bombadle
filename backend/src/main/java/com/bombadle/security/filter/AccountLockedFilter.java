package com.bombadle.security.filter;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.security.oauth2.CustomOAuth2PlayerUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountLockedFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isAuth = PATH_MATCHER.match("/api/auth/**", path);
        boolean isPlayersMeGet = PATH_MATCHER.match("/api/players/me", path) && "GET".equalsIgnoreCase(method);
        boolean isLeaderboard = PATH_MATCHER.match("/api/leaderboard/**", path);

        return isAuth || isPlayersMeGet || isLeaderboard;
    }

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

        if (isAccountLocked(authentication.getPrincipal())) {
            response.setStatus(423);
            response.setContentType("application/json");
            response.getWriter().write("{\"statusCode\":423,\"error\":\"Account Locked\",\"message\":\"Account is locked\"}");
            return;
        }

        filterChain.doFilter(request, response);
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