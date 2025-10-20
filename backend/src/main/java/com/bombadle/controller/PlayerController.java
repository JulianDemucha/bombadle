package com.bombadle.controller;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.mapper.PlayerMapper;
import com.bombadle.service.PlayerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
