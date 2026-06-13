package com.bombadle.controller;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.stats.LeaderboardService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/{gameMode}")
    ResponseEntity<Page<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable String gameMode,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(leaderboardService.getPagedLeaderboard(
                GameMode.valueOf(gameMode.toUpperCase()),
                page
        ));
    }

    @GetMapping("/{gameMode}/top3")
    List<LeaderboardEntryDto> getTop3Leaderboard(@PathVariable String gameMode) {
        return leaderboardService.getTop3Leaderboard(GameMode.valueOf(gameMode.toUpperCase()));
    }

    // todo: repair if needed
//    @GetMapping("/rank/{rank}")
//    public ResponseEntity<LeaderboardEntryDto> getScoreByRank(@PathVariable int rank) {
//        return leaderboardService.getPlayerByPositionInLeaderboard(rank)
//                .map(score -> mapper.toDto(score.getTodayScore()))
//                .map(ResponseEntity::ok)           // 200 OK
//                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 not found
//    }

    @GetMapping("/{gameMode}/player/{id}")
    public ResponseEntity<LeaderboardEntryDto> findPlayerRankById(
            @PathVariable String gameMode,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(leaderboardService.getRankedEntryByPlayerId(
                GameMode.valueOf(gameMode.toUpperCase()),
                id
        ));
    }
}