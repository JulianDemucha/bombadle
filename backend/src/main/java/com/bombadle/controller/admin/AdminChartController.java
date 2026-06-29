package com.bombadle.controller.admin;

import com.bombadle.dto.AdminChartResponse;
import com.bombadle.enums.ActivityChartDensity;
import com.bombadle.enums.ActivityChartPeriod;
import com.bombadle.service.admin.AdminChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/charts")
@RequiredArgsConstructor
public class AdminChartController {

    private final AdminChartService adminChartService;

    @GetMapping("/activity")
    public ResponseEntity<AdminChartResponse<?>> getActivityChart(
            @RequestParam(defaultValue = "TEN_MINUTES") ActivityChartDensity density,
            @RequestParam(defaultValue = "LAST_DAY") ActivityChartPeriod period,
            @RequestParam(defaultValue = "false") boolean combined
    ) {
        return ResponseEntity.ok(adminChartService.getActivityChart(density, period, combined));
    }
}
