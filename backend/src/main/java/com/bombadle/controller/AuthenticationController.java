package com.bombadle.controller;

import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.service.auth.*;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final CsrfCookieService csrfCookieService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookiesService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest registerRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String jwt = authenticationService.register(registerRequest);
        authCookieService.setAuthCookies(
                jwt,
                refreshTokenService.createRefreshToken(registerRequest.getEmail()).getRefreshToken(),
                httpResponse
        );
        csrfCookieService.ensureCsrfCookie(httpRequest, httpResponse);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @RequestBody AuthenticationRequest authRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String jwt = authenticationService.authenticate(authRequest);
        authCookieService.setAuthCookies(
                jwt,
                refreshTokenService.createRefreshToken(authRequest.getEmail()).getRefreshToken(),
                httpResponse
        );
        csrfCookieService.ensureCsrfCookie(httpRequest, httpResponse);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/email")
    public ResponseEntity<Map<String, Object>> checkPlayerByEmail(@RequestParam String email) {
        try {
            boolean exists = authenticationService.existsByEmail(email);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Unexpected error: " + ex.getMessage()));
        }
    }

    @GetMapping("/check/username")
    public ResponseEntity<Map<String, Object>> checkPlayerByUsername(@RequestParam String username) {
        try {
            boolean exists = authenticationService.existsByUsername(username);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Unexpected error: " + ex.getMessage()));
        }
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if(refreshToken == null) {
            return ResponseEntity.badRequest().body("refreshToken not found");
        }

        RefreshTokenCookieDto refreshTokenCookieDto = refreshTokenService.createRefreshTokenAndRevokeOld(refreshToken);

        authCookieService.setAuthCookies(refreshTokenCookieDto.getJwt(), refreshTokenCookieDto.getRefreshToken(), response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Optional<Cookie> refreshCookie = Optional.empty();
        if (request.getCookies() != null) {
            refreshCookie = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .findFirst();
        }

        refreshCookie.ifPresent(cookie -> {
            String token = cookie.getValue();
            refreshTokenService.revokeRefreshToken(token);
        });

        HttpHeaders clearCookiesHeaders = authCookieService.createClearCookiesHeaders();

        SecurityContextHolder.clearContext();

        return ResponseEntity
                .noContent()
                .headers(clearCookiesHeaders)
                .build();
    }

}


