package com.bombadle.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class StatelessCsrfFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_METHODS = List.of("POST", "PUT", "DELETE", "PATCH");
    private final List<String> excludedPathPrefixes;

    public StatelessCsrfFilter(List<String> excludedPathPrefixes) {
        this.excludedPathPrefixes = excludedPathPrefixes;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // excluded jeśli ścieżka zaczyna się od któregoś prefiksu (obsługuje query params)
        boolean excluded = excludedPathPrefixes.stream().anyMatch(path::startsWith);
        if (excluded) {
            filterChain.doFilter(request, response);
            return;
        }

        if (PROTECTED_METHODS.contains(request.getMethod())) {

            String headerToken = request.getHeader("X-XSRF-TOKEN");

            String cookieToken = null;
            if (request.getCookies() != null) {
                cookieToken = Arrays.stream(request.getCookies())
                        .filter(c -> "XSRF-TOKEN".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            if (headerToken == null || cookieToken == null || !headerToken.equals(cookieToken)) {
                log.warn("StatelessCsrfFilter: CSRF validation failed for path {}", path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
