package com.bombadle.service.player;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.PlayerDto;
import com.bombadle.entity.Score;
import com.bombadle.exception.UsernameAlreadyTakenException;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.stats.LeaderboardService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;
    private final PlayerDeletionService playerDeletionService;
    private final LeaderboardService leaderboardService;
    private final CacheManager cacheManager;

    public Optional<Player> findByEmail(String email){
        return repo.findByEmail(email);
    }

    public Optional<Player> findById(long id){
        return repo.findById(id);
    }

    public List<Player> getAllPlayers() {
        return repo.findAllByOrderByIdAsc();
    }

    public Page<Player> getAllPlayers(Pageable pageable) {
        return repo.findAllByOrderByIdAsc(pageable);
    }

    public PlayerDto getAuthenticatedPlayer(long playerId) {

        Player player = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User from token has NOT been found in the database: " + playerId
                ));
        return PlayerDto.toDto(player);
    }

    public void registerScore(Player player, Score score) {
        player.setHasGuessedToday(true);
        player.setTotalSuccessfulGuesses(player.getTotalSuccessfulGuesses() + 1);
        player.setTodayScore(score);
        repo.save(player);
    }

    @Transactional
    public void resetAllScores() {
        repo.resetAllScores();
        repo.flush();
    }

    @Transactional
    public PlayerDto updatePlayer(PlayerUpdateRequest request, long playerId) {

        // get player and update it with new values
        Player updatedPlayer = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException("User from token has NOT been found: " + playerId));

        boolean isPlayerInTop3 = leaderboardService.getTop3Leaderboard().stream()
                .anyMatch(entry -> entry.playerId().equals(playerId));

        // IF playerUpdatableDto.login() is NOT blank or null, change users email
        if (!isNullOrIsBlank(request.login())) {
            int length = request.login().length();
            if (length < 3 || length > 16)
                throw new IllegalArgumentException("Username must be between 3 and 16 characters");


            if (!updatedPlayer.getLogin().equals(request.login())) {
                /*
                    if provided login doesn't equal current login, check whether
                    the user with provided login already exists in the database
                */
                if (repo.existsByLogin(request.login()))
                    throw new UsernameAlreadyTakenException("Username " + request.login() + " already exists");

            }

            updatedPlayer.setLogin(request.login());
        }

        if (!isNullOrIsBlank(request.avatarImage())) {
            try {
                AvatarImage newAvatar = AvatarImage.valueOf(request.avatarImage());

                updatedPlayer.setAvatarImage(newAvatar);

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Avatar image '" + request.avatarImage() + "' not supported");
            }
        }

        repo.save(updatedPlayer);

        if (isPlayerInTop3) {
            log.info("Player {} is in top 3, clearing top-3-leaderboard cache", playerId);
            Objects.requireNonNull(cacheManager.getCache("top-3-leaderboard")).clear();
        }

        return PlayerDto.toDto(updatedPlayer);
    }

    public Player save(Player player) {
        return repo.save(player);
    }

    @Transactional
    public void deletePlayer(long playerId) {
        playerDeletionService.deletePlayerSelf(playerId);
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }
}
