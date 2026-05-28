package com.bombadle.dto;

import com.bombadle.entity.AnonymousGuessList;
import lombok.Builder;

import java.util.List;

@Builder
public record GuessListDto (List<GuessAttempt> guessList){
    public static GuessListDto toDto(AnonymousGuessList anonymousGuessList) {
        return GuessListDto.builder()
                .guessList(anonymousGuessList.getGuesses()
                ).build();
    }
}
