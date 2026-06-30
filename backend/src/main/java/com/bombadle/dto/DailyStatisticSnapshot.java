package com.bombadle.dto;

import com.bombadle.enums.GameMode;

import java.time.Instant;
import java.time.LocalDate;

public record DailyStatisticSnapshot(
        GameMode gameMode,
        LocalDate puzzleDate,
        Instant solvedAt,
        int numberOfTries,
        int leaderboardPosition,
        int totalParticipants
) {}
