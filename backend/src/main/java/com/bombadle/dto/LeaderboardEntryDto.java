package com.bombadle.dto;

import lombok.Builder;


@Builder
public record LeaderboardEntryDto(Long playerId,
                                  String playerAvatarImage,
                                  String scoreTimeStamp,
                                  int numberOfTries) {
}

