package com.bombadle.controller.admin;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.AdminCacheFlushRequest;
import com.bombadle.service.admin.AdminCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class AdminCacheController {
    private final AdminCacheService adminCacheService;

    @PostMapping("/flush")
    public ResponseEntity<Void> flushCache(
            @AuthenticationPrincipal PlayerPrincipal actor,
            @RequestBody AdminCacheFlushRequest request
    ) {
        adminCacheService.flushCache(actor.getPlayerId(), request);

        return ResponseEntity.ok().build();
    }
}