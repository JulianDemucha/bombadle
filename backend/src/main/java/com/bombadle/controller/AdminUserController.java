package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

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
}

