package com.bombadle.controller;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.service.game.GuessListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guess-list")
@RequiredArgsConstructor
public class GuessListController {
    private final GuessListService guessListService;

    @GetMapping("/player/{id}")
    public List<GuessAttempt> getGuessListByPlayerId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return guessListService.getGuessListByPlayerId(id);
    }
}
