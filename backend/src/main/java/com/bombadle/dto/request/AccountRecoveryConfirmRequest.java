package com.bombadle.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * newPassword is required only when the recovered account's authProvider is LOCAL
 * (validated in PlayerRecoveryService) — null/blank is expected and valid for OAUTH2_GOOGLE.
 */
public record AccountRecoveryConfirmRequest(
        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Niepoprawny format email")
        String email,

        @NotBlank(message = "Kod jest wymagany")
        @Size(min = 6, max = 6, message = "Kod musi mieć dokładnie 6 znaków")
        String code,

        @Size(min = 8, max = 24, message = "Password must be between 8 and 24 characters")
        String newPassword
) {
}
