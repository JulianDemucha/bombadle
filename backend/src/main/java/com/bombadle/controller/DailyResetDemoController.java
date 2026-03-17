package com.bombadle.controller;

import com.bombadle.service.scheduling.DailyResetService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-reset")
@AllArgsConstructor
public class DailyResetDemoController {
    private final DailyResetService dailyResetService;

    @PostMapping("/trigger")
    public String triggerDailyReset() {
        dailyResetService.pickNewCharacterCardAndResetScores();
        return "Daily reset triggered successfully!";
    }
}
