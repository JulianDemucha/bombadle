package com.bombadle.dto;

import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.enums.GameMode;
import lombok.Builder;

import java.util.List;

@Builder
public record GuessListDto(List<GuessAttempt> guessList) {

    public static GuessListDto toDto(AnonymousGuessList anonymousGuessList, GameMode gameMode) {
        return GuessListDto.builder()
                .guessList(anonymousGuessList.getGuesses().getOrDefault(gameMode, List.of()))
                .build();
    }

    public static GuessListDto fromList(List<GuessAttempt> attempts) {
        return GuessListDto.builder()
                .guessList(attempts)
                .build();
    }
}