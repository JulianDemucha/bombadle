package com.bombadle.dto;

import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import lombok.Builder;

import java.time.Instant;
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
        Map<GameMode, GuessListDto> mappedGuesses = anonymousSession.getGuessList().getGuesses().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> GuessListDto.fromList(entry.getValue())
                ));

        return AnonymousSessionDto.builder()
                .id(anonymousSession.getId())
                .guessLists(mappedGuesses)
                .completedModesToday(anonymousSession.getCompletedModesToday())
                .scoreTimestamps(anonymousSession.getScoreTimestamps())
                .build();
    }
}