package com.bombadle.dto;

import com.bombadle.entity.AnonymousGuessList;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
public record GuessListDto(List<GuessAttempt> guessList) {

    public static GuessListDto toDto(AnonymousGuessList anonymousGuessList) {
        if (anonymousGuessList == null || anonymousGuessList.getGuesses() == null) {
            return new GuessListDto(Collections.emptyList());
        }

        return GuessListDto.builder()
                .guessList(anonymousGuessList.getGuesses())
                .build();
    }

    public static GuessListDto fromList(List<GuessAttempt> attempts) {
        if (attempts == null) {
            return new GuessListDto(Collections.emptyList());
        }

        return GuessListDto.builder()
                .guessList(attempts)
                .build();
    }
}