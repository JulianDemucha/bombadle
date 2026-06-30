package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.enums.GameMode;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousGuessRegistrationService {
    private final AnonymousSessionService anonymousSessionService;
    private final CacheService cacheService;

    @Transactional
    public UUID registerGuessAndGetSessionId(AnonymousSession anonymousSession, GuessAttempt guessAttempt, GameMode gameMode) {

        AnonymousGuessList guessList = anonymousSession.getGuessListForMode(gameMode)
                .orElseGet(() -> {
                    AnonymousGuessList newList = AnonymousGuessList.builder()
                            .gameMode(gameMode)
                            .build();
                    anonymousSession.addGuessList(newList);
                    return newList;
                });

        guessList.addGuess(guessAttempt);

        if (guessAttempt.isCorrect()) {
            anonymousSession.markModeAsCompleted(gameMode);
            anonymousSession.addScoreTimestamp(gameMode, Instant.now());
            cacheService.evictCacheEntry("today-solvers", gameMode.name());
        }

        anonymousSession.setLastActiveAt(Instant.now());

        return anonymousSessionService.save(anonymousSession).getId();
    }
}