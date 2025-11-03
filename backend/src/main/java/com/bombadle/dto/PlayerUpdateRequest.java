package com.bombadle.dto;

import lombok.Builder;

@Builder
public record PlayerUpdateRequest(
    String login,
    String avatarImage
){}
