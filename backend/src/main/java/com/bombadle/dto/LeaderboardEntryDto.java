package com.bombadle.dto;

import lombok.Builder;
import lombok.Data;


@Builder
public record LeaderboardEntryDto(String playerLogin,
                                  String playerAvatarImage,
                                  String scoreTimeStamp,
                                  int numberOfTries) {
}

