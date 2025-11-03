package com.bombadle.controller;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.PlayerUpdateRequest;
import com.bombadle.dto.mapper.PlayerMapper;
import com.bombadle.service.PlayerService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@AllArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    private final PlayerMapper playerMapper;

    @GetMapping("/all")
    public List<PlayerDto> getAllPlayers() {
        return playerMapper.toDto(playerService.getAllPlayers());
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerDto> getAuthenticatedPlayer(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return playerService.getAuthenticatedPlayer(authentication);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updatePlayer(
            @NonNull @RequestBody PlayerUpdateRequest playerUpdateRequest,
            @CookieValue(name = "jwt") String jwt
    ) {
        return playerService.updatePlayer(playerUpdateRequest, jwt);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deletePlayer(@CookieValue(name = "jwt") String jwt) {
        return playerService.deletePlayer(jwt);
    }

}
