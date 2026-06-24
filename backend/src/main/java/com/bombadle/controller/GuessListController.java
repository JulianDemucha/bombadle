package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.GuessListDto;
import com.bombadle.enums.GameMode;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api/guess-list")
@RequiredArgsConstructor
public class GuessListController {
    private final GuessListService guessListService;
    private final AnonymousSessionService anonymousSessionService;

    @GetMapping("/{gameMode}")
    public ResponseEntity<GuessListDto> getMyGuessList(
            @PathVariable String gameMode,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        GameMode mode = GameMode.valueOf(gameMode.toUpperCase());

        if (userDetails != null) {
            return ResponseEntity.ok(
                    guessListService.getByPlayerId(userDetails.getPlayerId(), mode)
            );
        }

        if (anonymousSessionId != null) {
            return ResponseEntity.ok(
                    anonymousSessionService.getGuessList(anonymousSessionId, mode)
            );
        }

        return ResponseEntity.ok(new GuessListDto(Collections.emptyList()));
    }

    @GetMapping("/{gameMode}/player/{playerId}")
    public ResponseEntity<GuessListDto> getGuessListByPlayerId(
            @PathVariable Long playerId,
            @PathVariable String gameMode
    ) {
        return ResponseEntity.ok(
                guessListService.getByPlayerId(playerId, GameMode.valueOf(gameMode.toUpperCase()))
        );
    }
}