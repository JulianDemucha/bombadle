package com.bombadle.service.player;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.service.cache.CacheService;
import com.bombadle.service.stats.LeaderboardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerUpdateService {
    private final PlayerRepository repo;
    private final CacheService cacheService;
    private final LeaderboardService leaderboardService;

    @Transactional
    public PlayerDto updatePlayer(PlayerUpdateRequest request, long playerId) {

        Player updatedPlayer = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException("User from token has NOT been found: " + playerId));

        boolean isPlayerInTop3 = Arrays.stream(GameMode.values())
                .anyMatch(mode -> leaderboardService.getTop3Leaderboard(mode).stream()
                        .anyMatch(entry -> entry.playerId().equals(playerId)));

        boolean profileChanged = false;

        // Zmiana loginu - wykonaj tylko jeśli w żądaniu podano nowy login
        if (!isNullOrIsBlank(request.login())) {
            String normalizedUsername = request.login().toLowerCase();

            // Sprawdzenie zajętości nazwy przed walidacją długości
            if (!Objects.equals(updatedPlayer.getLogin(), normalizedUsername) && repo.existsByLogin(normalizedUsername)) {
                throw new RegistrationConflictException("Nazwa użytkownika jest zajęta");
            }

            int length = request.login().length();
            if (length < 3 || length > 16) {
                throw new IllegalArgumentException("Nazwa użytkownika musi być dłuższa niż 3 i krótsza niż 16 znaków");
            }

            if (!updatedPlayer.getLogin().equals(normalizedUsername)) {
                updatedPlayer.setDisplayName(request.login());
                updatedPlayer.setLogin(normalizedUsername);
                profileChanged = true;
            }
        }

        // Zmiana awatara
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
            log.info("Player {} updated profile, clearing paged and top-3 leaderboard caches", playerId);
            cacheService.clear("paged-leaderboard");
            cacheService.clear("top-3-leaderboard");
        } else if (isPlayerInTop3) {
            log.info("Player {} is in top 3, clearing top-3-leaderboard cache", playerId);
            cacheService.clear("top-3-leaderboard");
        }

        return PlayerDto.toDto(updatedPlayer);
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }
}