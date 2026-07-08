package com.bombadle.dto.request;

import com.bombadle.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 24, message = "Password must be between 8 and 24 characters")
        @StrongPassword
        String password
) {}
