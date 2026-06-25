package com.bombadle.controller;

import com.bombadle.dto.FullLeaderboardEntryDto;
import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.TodaySolversDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.stats.LeaderboardService;
import com.bombadle.service.stats.TodaySolversService;
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
    private final TodaySolversService todaySolversService;

    @GetMapping("/{gameMode}")
    ResponseEntity<Page<FullLeaderboardEntryDto>> getLeaderboard(
            @PathVariable String gameMode,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(leaderboardService.getPagedLeaderboard(
                resolveGameMode(gameMode),
                page
        ));
    }

    @GetMapping("/{gameMode}/top3")
    List<LeaderboardEntryDto> getTop3Leaderboard(@PathVariable String gameMode) {
        return leaderboardService.getTop3Leaderboard(GameMode.valueOf(gameMode.toUpperCase()));
    }

    @GetMapping("/{gameMode}/today-solvers")
    ResponseEntity<TodaySolversDto> getTodaySolvers(@PathVariable String gameMode) {
        return ResponseEntity.ok(todaySolversService.getTodaySolvers(resolveGameMode(gameMode)));
    }

    /**
     * Resolves the {gameMode} path variable to the canonical GameMode used by the paged leaderboard,
     * collapsing both the "quotes" alias and QUOTES_STAGE_1 onto QUOTES_STAGE_2 so the counts align
     * with the leaderboard the user is looking at.
     */
    private GameMode resolveGameMode(String gameMode) {
        if ("quotes".equalsIgnoreCase(gameMode))
            return GameMode.QUOTES_STAGE_2;

        GameMode gameModeEnum = GameMode.valueOf(gameMode.toUpperCase());

        if (gameModeEnum == GameMode.QUOTES_STAGE_1)
            return GameMode.QUOTES_STAGE_2;

        return gameModeEnum;
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