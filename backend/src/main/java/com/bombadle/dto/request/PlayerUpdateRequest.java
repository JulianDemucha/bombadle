package com.bombadle.dto.request;

import lombok.Builder;

@Builder
public record PlayerUpdateRequest(
    String login,
    String avatarImage
){}
