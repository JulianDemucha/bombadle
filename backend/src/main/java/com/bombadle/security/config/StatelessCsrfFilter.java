package com.bombadle.security.config;

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

    private static final List<String> PROTECTED_METHODS = Arrays.asList("POST", "PUT", "DELETE", "PATCH");

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/authenticate",
            "/api/auth/register",
            "/api/auth/csrf"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        log.info("StatelessCsrfFilter: Checking request {} {}", request.getMethod(), request.getRequestURI());

        String path = request.getRequestURI();
        if (EXCLUDED_PATHS.contains(path)) {
            log.info("StatelessCsrfFilter: Path {} is excluded from CSRF protection", path);
            filterChain.doFilter(request, response);
            return;
        }

        if (PROTECTED_METHODS.contains(request.getMethod())) {
            log.info("StatelessCsrfFilter: Protected method [{}]. Starting validation...", request.getMethod());

            String headerToken = request.getHeader("X-XSRF-TOKEN");
            String cookieToken = null;
            if (request.getCookies() != null) {
                cookieToken = Arrays.stream(request.getCookies())
                        .filter(c -> "XSRF-TOKEN".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            log.debug("StatelessCsrfFilter: Header Token: {}", headerToken);
            log.debug("StatelessCsrfFilter: Cookie Token: {}", cookieToken);

            if (headerToken == null || cookieToken == null || !headerToken.equals(cookieToken)) {
                log.warn("StatelessCsrfFilter: Invalid Token (Header: [{}], Cookie: [{}])", headerToken, cookieToken);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
                return;
            }

            log.info("StatelessCsrfFilter: Tokens match. Allowing request.");
        }

        filterChain.doFilter(request, response);
    }
}