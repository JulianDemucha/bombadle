package com.bombadle.controller;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.dto.GuessListDto;
import com.bombadle.dto.GuessResponse;
import com.bombadle.service.game.CardMatchingService;
import com.bombadle.service.game.CharacterCardService;
import com.bombadle.service.game.GuessListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/card-guessing")
@RequiredArgsConstructor
public class CardGuessingController {
    private final CardMatchingService cardMatchingService;
    private final CharacterCardService characterCardService;
    private final GuessListService guessListService;


    ///
/// Returns an GuessResponse which has CardField parameters - for example:
/// ```json
/// "correct": "false",
/// {
///     "name": {
///         "value": "Sebastian Bąk",
///         "match": "NOT_MATCH"
///     },
///     "gender": {
///         "value": "MALE",
///         "match": "MATCH"
///     },
///     "race": {
///         "value": "Czlowiek",
///         "match": "MATCH"
///     },
///     "alive": {
///         "value": true,
///         "match": "MATCH"
///     },
///     "colors": {
///         "value": [
///             "ZIELONY"
///         ],
///         "match": "NOT_MATCH"
///     },
///     "affiliations": {
///         "value": [
///             "Gwiezdna_Flota",
///             "Szeregowy_Gwiezdnej_Floty"
///         ],
///         "match": "NOT_FULL_MATCH"
///     },
///     "firstAppearanceEpisode": {
///         "value": 1,
///         "match": "MATCH"
///     }
/// }
/// ```
    @PostMapping("/classic/guess/{id}")
    public GuessResponse compareCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return cardMatchingService.compareCharacterCardClassic(id, userDetails);
    }

    @GetMapping("/classic/guess-list")
    public GuessListDto getGuessList(@AuthenticationPrincipal UserDetails userDetails) {
        return guessListService.getGuessListByUserDetails(userDetails);
    }
}
