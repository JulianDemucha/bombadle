package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.BasicStatisticsDto;
import com.bombadle.dto.DailyStatisticDto;
import com.bombadle.dto.DetailedStatisticsDto;
import com.bombadle.service.stats.PlayerStatisticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players/me/statistics")
@AllArgsConstructor
public class StatisticsController {

    private final PlayerStatisticsService playerStatisticsService;

    @GetMapping("/basic")
    public ResponseEntity<BasicStatisticsDto> getBasicStatistics(@AuthenticationPrincipal PlayerPrincipal userDetails) {
        return ResponseEntity.ok(playerStatisticsService.getBasicStatistics(userDetails.getPlayerId()));
    }

    @GetMapping("/detailed")
    public ResponseEntity<DetailedStatisticsDto> getDetailedStatistics(@AuthenticationPrincipal PlayerPrincipal userDetails) {
        return ResponseEntity.ok(playerStatisticsService.getDetailedStatistics(userDetails.getPlayerId()));
    }

    @GetMapping("/chart")
    public ResponseEntity<List<DailyStatisticDto>> getChartStatistics(@AuthenticationPrincipal PlayerPrincipal userDetails) {
        return ResponseEntity.ok(playerStatisticsService.getChartStatistics(userDetails.getPlayerId()));
    }
}
