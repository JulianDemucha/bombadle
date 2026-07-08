package com.bombadle.dto.request;

import com.bombadle.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 16, message = "Username must be between 3 and 16 characters")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 24, message = "Password must be between 8 and 24 characters")
        @StrongPassword
        String password
) {}
