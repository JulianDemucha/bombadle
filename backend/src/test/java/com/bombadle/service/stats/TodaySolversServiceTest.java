package com.bombadle.service.stats;

import com.bombadle.dto.TodaySolversDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodaySolversServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private AnonymousSessionService anonymousSessionService;

    @InjectMocks
    private TodaySolversService todaySolversService;

    @Test
    void getTodaySolvers_aggregatesLoggedInAndAnonymousCounts() {
        // ARRANGE
        GameMode gameMode = GameMode.CLASSIC;
        when(playerService.countSolversForMode(gameMode)).thenReturn(7L);
        when(anonymousSessionService.countSolversForMode(gameMode)).thenReturn(3L);

        // ACT
        TodaySolversDto result = todaySolversService.getTodaySolvers(gameMode);

        // ASSERT
        assertEquals(7L, result.loggedIn());
        assertEquals(3L, result.anonymous());
        verify(playerService).countSolversForMode(gameMode);
        verify(anonymousSessionService).countSolversForMode(gameMode);
    }
}
