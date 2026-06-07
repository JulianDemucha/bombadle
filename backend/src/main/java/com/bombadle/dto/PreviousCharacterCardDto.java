package com.bombadle.dto;

import lombok.Builder;

@Builder
public record PreviousCharacterCardDto(String name, String imageSrc) {
}
