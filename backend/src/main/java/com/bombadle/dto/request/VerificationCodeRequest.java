package com.bombadle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationCodeRequest(
        @NotBlank(message = "Kod jest wymagany")
        @Size(min = 6, max = 6, message = "Kod musi mieć dokładnie 6 znaków")
        String code,
        boolean deleteAllDataNow
) {}
