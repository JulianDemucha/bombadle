package com.bombadle.controller;

import com.bombadle.dto.TodaySolversDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.stats.LeaderboardService;
import com.bombadle.service.stats.TodaySolversService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private TodaySolversService todaySolversService;

    @InjectMocks
    private LeaderboardController controller;

    @Test
    void getTodaySolvers_quotesAlias_resolvesToStageTwo() {
        TodaySolversDto dto = new TodaySolversDto(1L, 2L);
        when(todaySolversService.getTodaySolvers(GameMode.QUOTES_STAGE_2)).thenReturn(dto);

        var response = controller.getTodaySolvers("quotes");

        assertEquals(dto, response.getBody());
        verify(todaySolversService).getTodaySolvers(GameMode.QUOTES_STAGE_2);
    }

    @Test
    void getTodaySolvers_quotesStageOne_resolvesToStageTwo() {
        when(todaySolversService.getTodaySolvers(GameMode.QUOTES_STAGE_2))
                .thenReturn(new TodaySolversDto(0L, 0L));

        controller.getTodaySolvers("QUOTES_STAGE_1");

        verify(todaySolversService).getTodaySolvers(GameMode.QUOTES_STAGE_2);
    }

    @Test
    void getTodaySolvers_classic_resolvesToClassic() {
        when(todaySolversService.getTodaySolvers(GameMode.CLASSIC))
                .thenReturn(new TodaySolversDto(0L, 0L));

        controller.getTodaySolvers("classic");

        verify(todaySolversService).getTodaySolvers(GameMode.CLASSIC);
    }

    @Test
    void getLeaderboard_quotesAlias_resolvesToStageTwo() {
        when(leaderboardService.getPagedLeaderboard(GameMode.QUOTES_STAGE_2, 0)).thenReturn(Page.empty());

        controller.getLeaderboard("quotes", 0);

        verify(leaderboardService).getPagedLeaderboard(GameMode.QUOTES_STAGE_2, 0);
    }
}
