package com.bombadle.controller.admin;

import com.bombadle.dto.AdminChartResponse;
import com.bombadle.enums.ActivityChartDensity;
import com.bombadle.enums.ActivityChartPeriod;
import com.bombadle.enums.DailySolversChartDensity;
import com.bombadle.enums.DailySolversChartPeriod;
import com.bombadle.enums.GameMode;
import com.bombadle.service.admin.AdminChartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminChartControllerTest {

    @Mock
    private AdminChartService adminChartService;

    @InjectMocks
    private AdminChartController controller;

    @Test
    void getActivityChart_delegatesToServiceWithGivenParams_andReturns200() {
        AdminChartResponse<?> mockResponse = new AdminChartResponse<>(List.of());
        doReturn(mockResponse).when(adminChartService)
                .getActivityChart(ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_WEEK, true);

        ResponseEntity<AdminChartResponse<?>> response = controller.getActivityChart(
                ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_WEEK, true);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
        verify(adminChartService).getActivityChart(ActivityChartDensity.HOUR, ActivityChartPeriod.LAST_WEEK, true);
    }

    @Test
    void getDailySolversChart_delegatesToServiceWithGivenParams_andReturns200() {
        AdminChartResponse<?> mockResponse = new AdminChartResponse<>(List.of());
        doReturn(mockResponse).when(adminChartService).getDailySolversChart(
                DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_3_MONTHS,
                GameMode.IMAGES, true);

        ResponseEntity<AdminChartResponse<?>> response = controller.getDailySolversChart(
                DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_3_MONTHS,
                GameMode.IMAGES, true);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
        verify(adminChartService).getDailySolversChart(
                DailySolversChartDensity.WEEK, DailySolversChartPeriod.LAST_3_MONTHS,
                GameMode.IMAGES, true);
    }

    @Test
    void getDailySolversChart_quotesStage1_delegatesToService() {
        AdminChartResponse<?> emptyResponse = new AdminChartResponse<>(List.of());
        doReturn(emptyResponse).when(adminChartService).getDailySolversChart(
                DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                GameMode.QUOTES_STAGE_1, false);

        ResponseEntity<AdminChartResponse<?>> response = controller.getDailySolversChart(
                DailySolversChartDensity.DAY, DailySolversChartPeriod.LAST_MONTH,
                GameMode.QUOTES_STAGE_1, false);

        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }
}
