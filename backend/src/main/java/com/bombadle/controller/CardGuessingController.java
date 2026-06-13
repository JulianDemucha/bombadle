package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.dto.response.GuessResponse;
import com.bombadle.enums.GameMode;
import com.bombadle.service.auth.cookie.AuthCookiesService;
import com.bombadle.service.game.GameServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController()
@RequestMapping("/api/card-guessing")
@RequiredArgsConstructor
public class CardGuessingController {
    private final AuthCookiesService authCookiesService;
    private final GameServiceFacade gameServiceFacade;

    @PostMapping("/{gameMode}/guess/{id}")
    public GuessResponse compareCard(
            @PathVariable String gameMode,
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        return gameServiceFacade.play(
                id,
                userDetails.getPlayerId(),
                GameMode.valueOf(gameMode.toUpperCase())
        );
    }

    @PostMapping("/{gameMode}/anonymous-guess/{id}")
    public ResponseEntity<?> compareCardAnonymous(
            @PathVariable String gameMode,
            @PathVariable Long id,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        if (userDetails != null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only for non-logged in users");
        }

        AnonymousGuessResponse anonymousGuessResponse =
                gameServiceFacade.playAnonymous(
                        id,
                        anonymousSessionId,
                        GameMode.valueOf(gameMode.toUpperCase())
                );

        if (anonymousSessionId == null) {
            ResponseCookie cookie = authCookiesService.createAnonymousSessionCookie(
                    anonymousGuessResponse.anonymousSessionId().toString()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(anonymousGuessResponse.guessResponse());
        }

        return ResponseEntity.ok(anonymousGuessResponse.guessResponse());
    }
}