package com.bombadle.controller;

import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.service.AuthenticationService;
import com.bombadle.dto.request.AuthenticationRequest;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.service.CookieService;
import com.bombadle.service.CsrfCookieService;
import com.bombadle.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest registerRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String jwt = authenticationService.register(registerRequest);
        cookieService.setAuthCookies(
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
        cookieService.setAuthCookies(
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
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response
    ) {
        if(refreshToken == null) {
            return ResponseEntity.badRequest().body("refreshToken not found");
        }

        RefreshTokenCookieDto refreshTokenCookieDto = refreshTokenService.createRefreshTokenAndRevokeOld(refreshToken);

        cookieService.setAuthCookies(refreshTokenCookieDto, response);

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

        cookieService.clearCookies(response);

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

}


