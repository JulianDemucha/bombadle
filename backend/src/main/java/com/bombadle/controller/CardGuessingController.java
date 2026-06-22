package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.AnonymousQuoteGameStateDto;
import com.bombadle.dto.QuotePromptDto;
import com.bombadle.dto.QuotesGameStateDto;
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
    public GuessResponse play(
            @PathVariable String gameMode,
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        GameMode mode = gameMode.equals("quotes") ?
                GameMode.QUOTES_STAGE_2
                :
                GameMode.valueOf(gameMode.toUpperCase());

        return gameServiceFacade.play(
                id,
                userDetails.getPlayerId(),
                mode
        );
    }

    // todo merge play and playAnonymous into one endpoint
    @PostMapping("/{gameMode}/anonymous-guess/{id}")
    public ResponseEntity<?> playAnonymous(
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

        GameMode mode = gameMode.equals("quotes") ?
                GameMode.QUOTES_STAGE_2
                :
                GameMode.valueOf(gameMode.toUpperCase());

        AnonymousGuessResponse anonymousGuessResponse =
                gameServiceFacade.playAnonymous(
                        id,
                        anonymousSessionId,
                        mode
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


    @PostMapping("/quotes/guess")
    public ResponseEntity<GuessResponse> playQuotesStageOne(
            @AuthenticationPrincipal PlayerPrincipal userDetails,
            @RequestParam String guess
    ) {
        return ResponseEntity.ok(gameServiceFacade.playQuotesStageOne(
                guess,
                userDetails.getPlayerId()
        ));
    }

    @PostMapping("/anonymous/quotes/guess")
    public ResponseEntity<?> playAnonymousQuotesStageOne(
            @AuthenticationPrincipal PlayerPrincipal userDetails,
            @RequestParam String guess,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId
    ) {
        if (userDetails != null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only for non-logged in users");
        }

        AnonymousGuessResponse anonymousGuessResponse =
                gameServiceFacade.playAnonymousQuotesStageOne(
                        guess,
                        anonymousSessionId
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

    @GetMapping("/quotes/prompt")
    public ResponseEntity<?> getQuotePrompt(
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        if (userDetails != null) {
            return ResponseEntity.ok(
                    gameServiceFacade.getQuotesGameStateForPlayer(userDetails.getPlayerId())
            );
        }

        AnonymousQuoteGameStateDto gameState =
                gameServiceFacade.getQuotesGameStateForAnonymous(anonymousSessionId);

        if (anonymousSessionId == null) {
            ResponseCookie cookie = authCookiesService.createAnonymousSessionCookie(
                    gameState.anonymousSessionId().toString()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(gameState);
        }

        return ResponseEntity.ok(gameState);
    }
}