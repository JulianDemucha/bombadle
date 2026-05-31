package com.bombadle.controller.admin;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.AdminPendingCardChangeDto;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.service.admin.AdminCharacterCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCharacterCardController {
    private final AdminCharacterCardService adminCharacterCardService;
    private final CharacterCardRepository characterCardRepository;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Void> createCard(
            @AuthenticationPrincipal PlayerPrincipal actor,
            @RequestPart("card") AdminCharacterCardRequest request,
            @RequestPart("image") MultipartFile image
    ) throws Exception {
        adminCharacterCardService.enqueueCreate(actor.getPlayerId(), request, image);
        return ResponseEntity.accepted().build();
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Void> updateCard(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor,
            @RequestPart("card") AdminCharacterCardRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {
        String currentName = characterCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Character card not found: " + id))
                .getName();
        adminCharacterCardService.enqueueUpdate(actor.getPlayerId(), id, request, image, currentName);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminCharacterCardService.enqueueDelete(actor.getPlayerId(), id);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/pending/create")
    public ResponseEntity<Void> cancelCreate(
            @AuthenticationPrincipal PlayerPrincipal actor,
            @RequestParam("name") String name
    ) {
        adminCharacterCardService.cancelCreate(actor.getPlayerId(), name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pending/update/{id}")
    public ResponseEntity<Void> cancelUpdate(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminCharacterCardService.cancelUpdate(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pending/delete/{id}")
    public ResponseEntity<Void> cancelDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal PlayerPrincipal actor
    ) {
        adminCharacterCardService.cancelDelete(actor.getPlayerId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminPendingCardChangeDto>> listPendingChanges() {
        return ResponseEntity.ok(adminCharacterCardService.listPendingChanges());
    }
}
