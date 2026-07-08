package com.bombadle.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountRecoveryRequest(
        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Niepoprawny format email")
        String email
) {
}
