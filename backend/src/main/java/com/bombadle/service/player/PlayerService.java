package com.bombadle.service.player;

import com.bombadle.dto.PlayerDto;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;

    public Optional<Player> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return repo.findByEmail(email.toLowerCase());
    }

    public Optional<Player> findByLoginNormalized(String login) {
        if (login == null) return Optional.empty();
        return repo.findByLogin(login.toLowerCase());
    }

    public Optional<Player> findById(long id) {
        return repo.findById(id);
    }

    public Player getPlayerById(long id) {
        return findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }

    public List<Player> getAllPlayers() {
        return repo.findAllByOrderByIdAsc();
    }

    public Page<Player> getAllPlayers(Pageable pageable) {
        return repo.findAllByOrderByIdAsc(pageable);
    }

    public PlayerDto getAuthenticatedPlayerDto(long playerId) {

        Player player = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User from token has NOT been found in the database: " + playerId
                ));
        return PlayerDto.toDto(player);
    }

    public Long getPlayerCount() {
        return repo.count();
    }

    public List<Player> findAllByMarkedForDeletionAtBefore(Instant cutoff) {
        return repo.findAllByMarkedForDeletionAtBefore(cutoff);
    }

    public Player save(Player player) {
        return repo.save(player);
    }


    public void manualDelete(Player player) {
        repo.delete(player);
    }

    public void updateLastEmailSentAt(Long playerId) {
        repo.updateLastEmailSentAt(playerId);
    }


    public Boolean existsByLogin(String login) {
        if (login == null) return false;
        return repo.existsByLogin(login.toLowerCase());
    }

    public Boolean existsByEmail(String email) {
        if (email == null) return false;
        return repo.existsByEmail(email.toLowerCase());
    }

    /**
     * Number of logged-in players that solved the given mode today. Backed by the daily-reset
     * semantics of {@code completedModesToday}, so the count is inherently scoped to the current day.
     */
    public long countSolversForMode(GameMode gameMode) {
        return repo.countByCompletedModeToday(gameMode.name());
    }

}
