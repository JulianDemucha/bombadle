package com.bombadle.security.auth;

import com.bombadle.dto.PlayerDto;
import com.bombadle.security.auth.dto.AuthenticationRequest;
import com.bombadle.security.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        String jwt = authenticationService.register(request);
        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletResponse response
    ) {
        String jwt = authenticationService.authenticate(request);
        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error: " + ex.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<PlayerDto> status(Authentication authentication) {
        return authenticationService.getAuthenticatedPlayer(authentication);
    }
}
