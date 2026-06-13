package com.bombadle.service.game;

import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.UserAlreadyGuessedException;
import com.bombadle.exception.CharacterCardNotFoundException;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameServiceFacade {
    private final PlayerService playerService;
    private final CharacterCardService characterCardService;
    private final GameService classicGameService;

    @Transactional
    @CacheEvict(value = "guess-list", key = "#playerId+ '-' + #gameMode")
    public GuessResponse play(
            Long guessCardId,
            long playerId,
            GameMode gameMode
    ) {
        Player player = playerService.findById(playerId).orElseThrow();

        CharacterCard guess = characterCardService.findCharacterCardById(guessCardId)
                .orElseThrow(() -> new CharacterCardNotFoundException(guessCardId));

        return classicGameService.play(
                guess,
                player,
                gameMode
        );
    }

    @Transactional
    public AnonymousGuessResponse playAnonymous(
            Long guessCardId,
            UUID anonymousSessionId,
            GameMode gameMode
    ) {
        CharacterCard guess = characterCardService.findCharacterCardById(guessCardId)
                .orElseThrow(() -> new CharacterCardNotFoundException(guessCardId));

        return classicGameService.playAnonymous(
                guess,
                anonymousSessionId,
                gameMode
        );
    }
}
