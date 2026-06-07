package com.bombadle.service.player;

import com.bombadle.entity.Player;
import com.bombadle.service.auth.cookie.RefreshTokenService;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.stats.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerCascadeDeletionService {
    private final PlayerService playerService;
    private final RefreshTokenService refreshTokenService;
    private final GuessListService guessListService;
    private final ScoreService scoreService;

    @Transactional
    public void deletePlayerWithCascade(Player player) {
        Long playerId = player.getId();

        guessListService.findByPlayerId(playerId).ifPresent(guessListService::manualDelete);
        refreshTokenService.findByPlayerId(playerId).ifPresent(refreshTokenService::manualDelete);
        scoreService.findByPlayerId(playerId).ifPresent(scoreService::manualDelete);
        playerService.manualDelete(player);
    }

}
