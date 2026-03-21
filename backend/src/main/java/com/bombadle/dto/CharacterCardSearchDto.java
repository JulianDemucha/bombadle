package com.bombadle.dto;

import com.bombadle.entity.CharacterCard;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record CharacterCardSearchDto(long id, String name, String imageSrc) {

    public static CharacterCardSearchDto toDto(CharacterCard characterCard) {
        return CharacterCardSearchDto.builder()
                .id(characterCard.getId())
                .name(characterCard.getName())
                .imageSrc(characterCard.getImageSrc())
                .build();
    }

    public static List<CharacterCardSearchDto> toDto(List<CharacterCard> characterCards) {
        return characterCards.stream()
                .map(CharacterCardSearchDto::toDto)
                .collect(Collectors.toList());
    }
}