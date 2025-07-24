package com.bombadle.controller;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.mapper.LeaderboardEntryMapper;
import com.bombadle.service.LeaderboardService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@AllArgsConstructor
public class LeaderboardController {
    private final LeaderboardService leaderboardService;
    private final LeaderboardEntryMapper mapper;

    @GetMapping("/full")
    List<LeaderboardEntryDto> getFullLeaderboard() {
        return mapper.ScoresToDto(leaderboardService.getSortedLeaderboard());
    }

    @GetMapping("/top10")
    List<LeaderboardEntryDto> getTop10Leaderboard() {
        return mapper.ScoresToDto(leaderboardService.getTop10Leaderboard());
    }

    @GetMapping("/rank/{rank}")
    public ResponseEntity<LeaderboardEntryDto> getScoreByRank(@PathVariable int rank) {
        return leaderboardService.getPlayerByPositionInLeaderboard(rank)
                .map(score -> mapper.toDto(score.getTodayScore()))
                .map(ResponseEntity::ok)           // 200 OK
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 not found
    }



}
