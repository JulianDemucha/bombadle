package com.bombadle.controller;

import com.bombadle.dto.LeaderboardEntryDto;
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

    @GetMapping
    ResponseEntity<Page<LeaderboardEntryDto>> getLeaderboard(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(leaderboardService.getPagedLeaderboard(page));
    }

    @GetMapping("/top3")
    List<LeaderboardEntryDto> getTop10Leaderboard() {
        return leaderboardService.getTop3Leaderboard();
    }

    // todo: repair if needed
//    @GetMapping("/rank/{rank}")
//    public ResponseEntity<LeaderboardEntryDto> getScoreByRank(@PathVariable int rank) {
//        return leaderboardService.getPlayerByPositionInLeaderboard(rank)
//                .map(score -> mapper.toDto(score.getTodayScore()))
//                .map(ResponseEntity::ok)           // 200 OK
//                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 not found
//    }

    @GetMapping("/player/{id}")
    public ResponseEntity<LeaderboardEntryDto> findPlayerRankById(@PathVariable Long id) {
        return ResponseEntity.ok(leaderboardService.getRankedEntryByPlayerId(id));
    }


}
