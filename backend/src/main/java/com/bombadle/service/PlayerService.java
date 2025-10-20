package com.bombadle.service;

import com.bombadle.entity.Player;
import com.bombadle.repository.PlayerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public List<Player> getAllPlayers() {
        return playerRepository.findAllByOrderByIdAsc();
    }
}
