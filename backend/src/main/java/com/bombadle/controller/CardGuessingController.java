package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.QuotesGameStateDto;
import com.bombadle.dto.response.AnonymousGuessResponse;
import com.bombadle.enums.GameMode;
import com.bombadle.service.auth.cookie.AuthCookiesService;
import com.bombadle.service.game.GameServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    // (Classic, Images, Quotes Stage 2)
    @PostMapping("/{gameMode}/guess/{id}")
    public ResponseEntity<?> play(
            @PathVariable String gameMode,
            @PathVariable Long id,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        GameMode mode = gameMode.equals("quotes") ?
                GameMode.QUOTES_STAGE_2 : GameMode.valueOf(gameMode.toUpperCase());

        if (userDetails != null) {
            return ResponseEntity.ok(
                    gameServiceFacade.play(id, userDetails.getPlayerId(), mode)
            );
        }

        AnonymousGuessResponse anonymousGuessResponse =
                gameServiceFacade.playAnonymous(id, anonymousSessionId, mode);

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
    public ResponseEntity<?> playQuotesStageOne(
            @RequestParam String guess,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        if (userDetails != null) {
            return ResponseEntity.ok(
                    gameServiceFacade.playQuotesStageOne(guess, userDetails.getPlayerId())
            );
        }

        AnonymousGuessResponse anonymousGuessResponse =
                gameServiceFacade.playAnonymousQuotesStageOne(guess, anonymousSessionId);

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
    public ResponseEntity<QuotesGameStateDto> getQuotePrompt(
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        if (userDetails != null) {
            return ResponseEntity.ok(
                    gameServiceFacade.getQuotesGameStateForPlayer(userDetails.getPlayerId())
            );
        }

        return ResponseEntity.ok(
                gameServiceFacade.getQuotesGameStateForAnonymous(anonymousSessionId)
        );
    }

    @GetMapping(value = "/images/current", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getCurrentImage(
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        Long playerId = (userDetails != null) ? userDetails.getPlayerId() : null;

        Resource resource = gameServiceFacade.getImagesModeCurrentResource(playerId, anonymousSessionId);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(resource);
    }
}