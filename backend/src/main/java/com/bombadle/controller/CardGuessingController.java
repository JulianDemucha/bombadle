package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.AnonymousGuessResponse;
import com.bombadle.dto.GuessListDto;
import com.bombadle.dto.GuessResponse;
import com.bombadle.service.auth.CookieService;
import com.bombadle.service.game.CardMatchingService;
import com.bombadle.service.game.GuessListService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController()
@RequestMapping("/api/card-guessing")
@RequiredArgsConstructor
public class CardGuessingController {
    private final CardMatchingService cardMatchingService;
    private final GuessListService guessListService;
    private final CookieService cookieService;


    /// Returns an GuessResponse which has CardField parameters - for example:
    /// ```json
    /// "correct": "false",
    ///{
    ///     "name": {
    ///         "value": "Sebastian Bąk",
    ///         "match": "NOT_MATCH"
    ///},
    ///     "gender": {
    ///         "value": "MALE",
    ///         "match": "MATCH"
    ///},
    ///     "race": {
    ///         "value": "Czlowiek",
    ///         "match": "MATCH"
    ///},
    ///     "alive": {
    ///         "value": true,
    ///         "match": "MATCH"
    ///},
    ///     "colors": {
    ///         "value": [
    ///             "ZIELONY"
    ///],
    ///         "match": "NOT_MATCH"
    ///},
    ///     "affiliations": {
    ///         "value": [
    ///             "Gwiezdna_Flota",
    ///             "Szeregowy_Gwiezdnej_Floty"
    ///],
    ///         "match": "NOT_FULL_MATCH"
    ///},
    ///     "firstAppearanceEpisode": {
    ///         "value": 1,
    ///         "match": "MATCH"
    ///}
    ///}
    ///```
    @PostMapping("/classic/guess/{id}")
    public GuessResponse compareCard(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        return cardMatchingService.compareCharacterCardClassic(id, userDetails.getPlayerId());
    }

    @PostMapping("/classic/anonymous-guess/{id}")
    public ResponseEntity<?> compareCardAnonymous(
            @PathVariable Long id,
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {
        if (userDetails != null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only for non-logged in users");
        }

        AnonymousGuessResponse anonymousGuessResponse = cardMatchingService.compareCharacterCardClassicAnonymous(id, anonymousSessionId);

        if (anonymousSessionId == null) {
            ResponseCookie cookie = cookieService.createCookie(
                    "ANON_SESSION_ID",
                    anonymousGuessResponse.anonymousSessionId().toString(),
                    60 * 60 * 24 //24h
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(anonymousGuessResponse.guessResponse());
        }

        return ResponseEntity.ok((anonymousGuessResponse).guessResponse());
    }
}
