package com.bombadle.service.player;
import com.bombadle.dto.request.ChangePasswordRequest;
import com.bombadle.entity.Player;
import com.bombadle.exception.InvalidCredentialsException;
import com.bombadle.exception.PasswordAlreadySetException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerCredentialsService {
    private final PlayerService playerService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void activateAccount(Long playerId) {
        Optional<Player> playerOpt = playerService.findById(playerId);

        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }

        Player player = playerOpt.get();
        player.setEmailVerified(true);

        playerService.save(player);
    }

    @Transactional
    public void changePassword(Long playerId, String newPassword) {
        Optional<Player> playerOpt = playerService.findById(playerId);
        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }
        Player player = playerOpt.get();
        player.setPasswordHash(passwordEncoder.encode(newPassword));
        playerService.save(player);
    }

    @Transactional
    public void changePasswordWithVerification(Long playerId, ChangePasswordRequest request) {
        Player player = playerService.getPlayerById(playerId);

        if (!passwordEncoder.matches(request.oldPassword(), player.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        player.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        playerService.save(player);

        log.info("Player {} successfully changed their password in profile settings.", player.getLogin());
    }

    // for OAuth2 users, that want to set up password
    @Transactional
    public void setPasswordIfBlank(Long playerId, String password) {
        Optional<Player> playerOpt = playerService.findById(playerId);
        if (playerOpt.isEmpty()) {
            throw new UsernameNotFoundException("User has NOT been found: " + playerId);
        }
        Player player = playerOpt.get();
        if (!player.getPasswordHash().isBlank()) {
            throw new PasswordAlreadySetException();
        }
        player.setPasswordHash(passwordEncoder.encode(password));
        playerService.save(player);
    }

    @Transactional
    public void recordEmailSent(Long playerId) {
        playerService.updateLastEmailSentAt(playerId);
    }
}
