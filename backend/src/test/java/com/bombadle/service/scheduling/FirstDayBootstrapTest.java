package com.bombadle.service.scheduling;

import com.bombadle.service.game.CurrentCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstDayBootstrapTest {

    @Mock
    private CurrentCardStateService currentCardStateService;

    @Mock
    private DailyResetService dailyResetService;

    @InjectMocks
    private FirstDayBootstrap bootstrap;

    @Test
    void initializeOnFreshDatabase_stateAlreadyExists_doesNothing() {
        when(currentCardStateService.hasState()).thenReturn(true);

        bootstrap.initializeOnFreshDatabase();

        verifyNoInteractions(dailyResetService);
    }

    @Test
    void initializeOnFreshDatabase_noState_picksFirstDay() {
        when(currentCardStateService.hasState()).thenReturn(false);

        bootstrap.initializeOnFreshDatabase();

        verify(dailyResetService).initializeFirstDay();
    }

    @Test
    void initializeOnFreshDatabase_pickingFails_doesNotPreventStartup() {
        when(currentCardStateService.hasState()).thenReturn(false);
        doThrow(new IllegalStateException("No quotes in the database"))
                .when(dailyResetService).initializeFirstDay();

        assertDoesNotThrow(() -> bootstrap.initializeOnFreshDatabase());
    }
}
