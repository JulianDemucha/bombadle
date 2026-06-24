package com.bombadle.service.game;

import com.bombadle.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreMaintenanceService {
    private final PlayerRepository playerRepository;
    @Transactional
    public void resetAllScores() {
        playerRepository.resetAllScores();
        playerRepository.flush();
    }
}
