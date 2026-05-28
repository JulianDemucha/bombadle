package com.bombadle.controller;

import com.bombadle.dto.GuessListDto;
import com.bombadle.service.game.GuessListService;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/guess-list")
@RequiredArgsConstructor
public class GuessListController {
    private final GuessListService guessListService;
    private final AnonymousSessionService anonymousSessionService;

    @GetMapping("/classic/player/{playerId}")
    public GuessListDto getGuessList(@PathVariable Long playerId) {
        return guessListService.getGuessListByPlayerId(playerId);
    }

//    @GetMapping("/classic/anonymous")
//    public GuessListDto getAnonymousGuessList(
//            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId
//    ) {
//        return anonymousSessionService.getGuessList(anonymousSessionId);
//    }


}
