package com.bombadle.security.filter;

import com.bombadle.service.CsrfCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CsrfCookieInjectionFilter extends OncePerRequestFilter {

    private final CsrfCookieService csrfCookieService;

    //manual injection
    public CsrfCookieInjectionFilter(CsrfCookieService csrfCookieService) {
        this.csrfCookieService = csrfCookieService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean authenticated =
                auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getPrincipal());

        if (authenticated) {
            csrfCookieService.ensureCsrfCookie(request, response);
        }

        filterChain.doFilter(request, response);
    }
}
