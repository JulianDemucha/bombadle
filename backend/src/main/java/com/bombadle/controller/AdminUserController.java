package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.AdminUserUpdateRequest;
import com.bombadle.service.admin.AdminUserService;
import com.bombadle.service.player.PlayerDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;
    private final PlayerDeletionService playerDeletionService;

    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminUserService.blockUser(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminUserService.unblockUser(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-delete")
    public ResponseEntity<Void> markForDeletion(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        playerDeletionService.markForDeletion(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel-delete")
    public ResponseEntity<Void> cancelDeletion(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        playerDeletionService.cancelDeletion(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        playerDeletionService.deletePlayerByAdmin(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminUserService.updateUser(actor.getPlayerId(), id, request);
        return ResponseEntity.noContent().build();
    }
}
