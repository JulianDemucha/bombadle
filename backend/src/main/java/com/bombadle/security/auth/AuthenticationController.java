package com.bombadle.security.auth;

import com.bombadle.security.auth.dto.AuthenticationRequest;
import com.bombadle.security.auth.dto.AuthenticationResponse;
import com.bombadle.security.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
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
}
