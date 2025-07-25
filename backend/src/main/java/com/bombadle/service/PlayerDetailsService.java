package com.bombadle.service;

import com.bombadle.dto.AuthenticatedPlayer;
import com.bombadle.entity.Player;
import com.bombadle.repository.PlayerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerDetailsService implements UserDetailsService {
    private final PlayerRepository repo;
    public PlayerDetailsService(PlayerRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Player player = repo.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Player not found: " + login));
        return new AuthenticatedPlayer(player);
    }
}