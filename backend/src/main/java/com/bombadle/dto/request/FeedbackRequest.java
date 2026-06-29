package com.bombadle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotBlank @Size(min = 3, max = 40) String title,
        @NotBlank @Size(min = 10, max = 700) String description
) {}
