package com.bombadle.dto;

import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
public record AnonymousSessionDto(
        UUID id,
        Map<GameMode, GuessListDto> guessLists,
        Set<GameMode> completedModesToday,
        Map<GameMode, Instant> scoreTimestamps
) {
    public static AnonymousSessionDto toDto(AnonymousSession anonymousSession) {

        Map<GameMode, GuessListDto> mappedGuesses = anonymousSession.getGuessLists() == null
                ? Collections.emptyMap()
                : anonymousSession.getGuessLists().stream()
                .collect(Collectors.toMap(
                        AnonymousGuessList::getGameMode,
                        list -> GuessListDto.fromList(list.getGuesses())
                ));

        return AnonymousSessionDto.builder()
                .id(anonymousSession.getId())
                .guessLists(mappedGuesses)
                .completedModesToday(anonymousSession.getCompletedModesToday())
                .scoreTimestamps(anonymousSession.getScoreTimestamps())
                .build();
    }
}