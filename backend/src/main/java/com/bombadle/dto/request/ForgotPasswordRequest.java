package com.bombadle.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestParam;

public record ForgotPasswordRequest(
        @RequestParam
        @Valid
        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Niepoprawny format email")
        String email
) {
}
