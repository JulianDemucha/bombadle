package com.bombadle.controller.admin;

import com.bombadle.dto.AdminAuditLogDto;
import com.bombadle.service.admin.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AdminAuditService adminAuditService;

    @GetMapping("/{id}")
    public ResponseEntity<AdminAuditLogDto> getAuditById(@PathVariable Long id) {
        return adminAuditService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<AdminAuditLogDto>> getAuditByActorId(
            @RequestParam("actorId") Long actorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction
    ) {
        Sort sort = Sort.by(
                Sort.Direction.fromString(direction),
                "createdAt"
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminAuditService.getByActorId(actorId, pageable));
    }
}
