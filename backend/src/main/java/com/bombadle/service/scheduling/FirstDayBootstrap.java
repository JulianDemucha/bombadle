package com.bombadle.service.scheduling;

import com.bombadle.service.game.CurrentCardStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** On a fresh database there is no daily state yet, so the game would be empty until the first 07:00 reset. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bombadle.daily-reset.enabled", havingValue = "true", matchIfMissing = true)
public class FirstDayBootstrap {

    private final CurrentCardStateService currentCardStateService;
    private final DailyResetService dailyResetService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeOnFreshDatabase() {
        if (currentCardStateService.hasState()) {
            return;
        }

        log.info("No daily game state found (fresh database): picking the first daily entities.");
        try {
            dailyResetService.initializeFirstDay();
        } catch (RuntimeException e) {
            // Not fatal: the regular 07:00 reset picks them
            log.warn("Could not pick the first daily entities: {}", e.getMessage());
        }
    }
}
