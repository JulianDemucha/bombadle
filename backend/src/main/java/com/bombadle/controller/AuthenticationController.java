package com.bombadle.controller;

import com.bombadle.dto.RefreshTokenCookieDto;
import com.bombadle.dto.request.*;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.*;
import com.bombadle.service.auth.cookie.AuthCookiesService;
import com.bombadle.service.auth.cookie.RefreshTokenService;
import com.bombadle.service.auth.email.EmailActionInitiator;
import com.bombadle.service.auth.email.EmailConfirmationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    private final RefreshTokenService refreshTokenService;
    private final AuthCookiesService authCookieService;
    private final PostLoginService postLoginService;
    private final EmailConfirmationService emailConfirmationService;
    private final EmailActionInitiator emailActionInitiator;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody @Valid RegisterRequest registerRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Player player = authenticationService.register(registerRequest);
        postLoginService.processSuccessfulLogin(httpRequest, httpResponse, player);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @RequestBody @Valid AuthenticationRequest authRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Player player = authenticationService.authenticate(authRequest);
        postLoginService.processSuccessfulLogin(httpRequest, httpResponse, player);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/initiate-verify-email")
    public ResponseEntity<Void> initiateVerifyEmail(
            @RequestBody @Valid InitiateVerifyEmailRequest request
    ) {
        emailActionInitiator.initiateAccountActivation(request.email());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestBody @Valid VerificationCodeWithEmailRequest request
    ) {
        emailConfirmationService.confirmEmailVerification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/initiate-reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        emailActionInitiator.initiatePasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-reset-password")
    public ResponseEntity<Void> confirmResetPassword(@RequestBody @Valid PasswordResetRequest request) {
        emailConfirmationService.confirmResetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/initiate-recover-account")
    public ResponseEntity<Void> initiateRecoverAccount(@RequestBody @Valid AccountRecoveryRequest request) {
        emailActionInitiator.initiateAccountRecovery(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-recover-account")
    public ResponseEntity<Void> confirmRecoverAccount(@RequestBody @Valid AccountRecoveryConfirmRequest request) {
        emailConfirmationService.confirmAccountRecovery(request);
        return ResponseEntity.ok().build();
    }

    //todo: switch to @RequestBody, so potential email won't log from url
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
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("refreshToken not found");
        }

        RefreshTokenCookieDto refreshTokenCookieDto = refreshTokenService.createRefreshTokenAndRevokeOld(refreshToken);

        authCookieService.setAuthCookies(refreshTokenCookieDto.getJwt(), refreshTokenCookieDto.getRefreshToken(), response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
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


