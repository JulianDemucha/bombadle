package com.bombadle.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryDto {
    private String playerLogin;
    private String playerAvatarImage;
    private String scoreTimeStamp;
    private int numberOfTries;
}
