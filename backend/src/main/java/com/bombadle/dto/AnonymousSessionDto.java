package com.bombadle.dto;

import com.bombadle.entity.AnonymousSession;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AnonymousSessionDto(UUID id, GuessListDto guessList, boolean hasGuessedToday, Instant scoreTimestamp)
{
    public static AnonymousSessionDto toDto(AnonymousSession anonymousSession) {
        return AnonymousSessionDto.builder()
                .id(anonymousSession.getId())
                .guessList(GuessListDto.toDto(anonymousSession.getGuessList()))
                .hasGuessedToday(anonymousSession.hasGuessedToday())
                .scoreTimestamp(anonymousSession.getScoreTimestamp())
                .build();
    }
}
