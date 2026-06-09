package com.bombadle.service.player;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.request.ChangePasswordRequest;
import com.bombadle.entity.Score;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.PasswordAlreadySetException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;
    private final LeaderboardService leaderboardService;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;

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

    public List<Player> findAllByMarkedForDeletionAtBefore(Instant cutoff) {
        return repo.findAllByMarkedForDeletionAtBefore(cutoff);
    }

    @Transactional
    public PlayerDto updatePlayer(PlayerUpdateRequest request, long playerId) {

        // get player and update it with new values
        Player updatedPlayer = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException("User from token has NOT been found: " + playerId));

        boolean isPlayerInTop3 = leaderboardService.getTop3Leaderboard().stream()
                .anyMatch(entry -> entry.playerId().equals(playerId));
        boolean profileChanged = false;

        // IF playerUpdatableDto.login() is NOT blank or null, change users login
        if (!isNullOrIsBlank(request.login())) {
            int length = request.login().length();
            if (length < 3 || length > 16)
                throw new IllegalArgumentException("Username must be between 3 and 16 characters");

            String normalizedLogin = request.login().toLowerCase();

            if (!updatedPlayer.getLogin().equals(normalizedLogin)) {
                /*
                    if provided login doesn't equal current login, check whether
                    the user with provided login already exists in the database
                */
                if (repo.existsByLogin(normalizedLogin))
                    throw new UsernameAlreadyTakenException("Username " + request.login() + " already exists");

            }

            updatedPlayer.setDisplayName(request.login());
            updatedPlayer.setLogin(normalizedLogin);
            profileChanged = true;
        }

        if (!isNullOrIsBlank(request.avatarImage())) {
            try {
                AvatarImage newAvatar = AvatarImage.valueOf(request.avatarImage());
                if (newAvatar != updatedPlayer.getAvatarImage()) {
                    updatedPlayer.setAvatarImage(newAvatar);
                    profileChanged = true;
                }

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Avatar image '" + request.avatarImage() + "' not supported");
            }
        }

        repo.save(updatedPlayer);

        if (profileChanged) {
            log.info("Player {} updated profile, clearing classic and top-3 leaderboard caches", playerId);
            var classicCache = cacheManager.getCache("classic-leaderboard");
            if (classicCache != null) {
                classicCache.clear();
            }
            var top3Cache = cacheManager.getCache("top-3-leaderboard");
            if (top3Cache != null) {
                top3Cache.clear();
            }
        } else if (isPlayerInTop3) {
            log.info("Player {} is in top 3, clearing top-3-leaderboard cache", playerId);
            Objects.requireNonNull(cacheManager.getCache("top-3-leaderboard")).clear();
        }

        return PlayerDto.toDto(updatedPlayer);
    }

    @Transactional
    public void activateAccount(Long playerId) {
        Optional<Player> playerOpt = findById(playerId);

        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }

        Player player = playerOpt.get();
        player.setEmailVerified(true);

        repo.save(player);
    }

    @Transactional
    public void changePassword(Long playerId, String newPassword) {
        Optional<Player> playerOpt = findById(playerId);
        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }
        Player player = playerOpt.get();
        player.setPasswordHash(passwordEncoder.encode(newPassword));
        repo.save(player);
    }

    @Transactional
    public void recordEmailSent(Long playerId) {
        repo.updateLastEmailSentAt(playerId);
    }

    @Transactional
    public void changePasswordWithVerification(Long playerId, ChangePasswordRequest request) {
        Player player = getPlayerById(playerId);

        if (!passwordEncoder.matches(request.oldPassword(), player.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        player.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        repo.save(player);

        log.info("Player {} successfully changed their password in profile settings.", player.getLogin());
    }

    // for OAuth2 users, that want to set up password
    @Transactional
    public void setPasswordIfBlank(Long playerId, String password) {
        Optional<Player> playerOpt = findById(playerId);
        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }
        Player player = playerOpt.get();
        if (!player.getPasswordHash().isBlank()) {
            throw new PasswordAlreadySetException();
        }
        player.setPasswordHash(passwordEncoder.encode(password));
        repo.save(player);
    }

    public Player save(Player player) {
        return repo.save(player);
    }


    public void manualDelete(Player player) {
        repo.delete(player);
    }


    public Boolean existsByLogin(String login) {
        if (login == null) return false;
        return repo.existsByLogin(login.toLowerCase());
    }

    public Boolean existsByEmail(String email) {
        if (email == null) return false;
        return repo.existsByEmail(email.toLowerCase());
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }
}
