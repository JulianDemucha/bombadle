package com.bombadle.service.stats;

import com.bombadle.dto.TodaySolversDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Aggregates the per-mode "solved today" counts from their owning domains: logged-in solvers from
 * {@link PlayerService#countSolversForMode} and anonymous solvers from
 * {@link AnonymousSessionService#countSolversForMode}. Both domains count the same way (by their
 * {@code completedModesToday} set), keeping the figures symmetric and aligned with the leaderboard.
 */
@Service
@RequiredArgsConstructor
public class TodaySolversService {

    private final PlayerService playerService;
    private final AnonymousSessionService anonymousSessionService;

    public TodaySolversDto getTodaySolvers(GameMode gameMode) {
        long loggedIn = playerService.countSolversForMode(gameMode);
        long anonymous = anonymousSessionService.countSolversForMode(gameMode);
        return new TodaySolversDto(loggedIn, anonymous);
    }
}
