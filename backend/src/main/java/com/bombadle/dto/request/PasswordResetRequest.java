package com.bombadle.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest (
        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Niepoprawny format email")
        String email,

        @NotBlank(message = "Kod jest wymagany")
        @Size(min = 6, max = 6, message = "Kod musi mieć dokładnie 6 znaków")
        String code,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 24, message = "Password must be between 8 and 24 characters")
        String newPassword
){}
