package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.request.ChangePasswordRequest;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.dto.request.SetPasswordRequest;
import com.bombadle.dto.request.VerificationCodeRequest;
import com.bombadle.entity.Player;
import com.bombadle.service.auth.cookie.CookieService;
import com.bombadle.service.auth.email.EmailActionInitiator;
import com.bombadle.service.auth.email.EmailConfirmationService;
import com.bombadle.service.player.AnonymousSessionService;
import com.bombadle.service.player.PlayerService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@AllArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    private final AnonymousSessionService anonymousSessionService;
    private final CookieService cookieService;
    private final EmailActionInitiator emailActionInitiator;
    private final EmailConfirmationService emailConfirmationService;

    @GetMapping("/all")
    public Page<PlayerDto> getAllPlayers(Pageable pageable) {
        return playerService.getAllPlayers(pageable).map(PlayerDto::toDto);
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerDto> getAuthenticatedPlayer(@AuthenticationPrincipal PlayerPrincipal userDetails) {
        return ResponseEntity.ok(playerService.getAuthenticatedPlayer(userDetails.getPlayerId()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updatePlayer(
            @NonNull @RequestBody PlayerUpdateRequest playerUpdateRequest,
            @AuthenticationPrincipal PlayerPrincipal userDetails
    ) {

        return ResponseEntity.ok(playerService.updatePlayer(playerUpdateRequest, userDetails.getPlayerId()));
    }

    @GetMapping("/anonymous/me")
    public ResponseEntity<AnonymousSessionDto> getAnonymousStatus(
            @CookieValue(value = "ANON_SESSION_ID", required = false) UUID anonymousSessionId
    ) {

        AnonymousSessionDto anonymousSessionDto = anonymousSessionService.getAnonymousSession(anonymousSessionId);
        if(anonymousSessionId == null || !anonymousSessionId.equals(anonymousSessionDto.id())){
            ResponseCookie cookie = cookieService.createCookie(
                    "ANON_SESSION_ID",
                    anonymousSessionDto.id().toString(),
                    60 * 60 * 24 //24h
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(anonymousSessionDto);
        }

        return ResponseEntity.ok(anonymousSessionDto);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal PlayerPrincipal principal) {

        playerService.changePasswordWithVerification(
                principal.getPlayerId(),
                request
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/set-up-password")
    public ResponseEntity<Void> setUpPasswordForOAuth2Player(
            @RequestBody @Valid SetPasswordRequest request,
            @AuthenticationPrincipal PlayerPrincipal principal
    ){
        playerService.setPasswordIfBlank(principal.getPlayerId(), request.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/delete-request")
    public ResponseEntity<Void> requestAccountDeletion(
            @AuthenticationPrincipal PlayerPrincipal principal) {

        Player player = playerService.getPlayerById(principal.getPlayerId());
        emailActionInitiator.initiateAccountDeletion(player);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/delete-confirm")
    public ResponseEntity<Void> confirmAccountDeletion(
            @Valid @RequestBody VerificationCodeRequest request,
            @AuthenticationPrincipal PlayerPrincipal principal) {

        emailConfirmationService.confirmPlayerSelfDeletion(request, principal);
        return ResponseEntity.ok().build();
    }


}
