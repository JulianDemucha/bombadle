package com.bombadle.service.player;

import com.bombadle.dto.DailyStatisticSnapshot;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.DeletedAccountStatistic;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.PlayerAuthProvider;
import com.bombadle.exception.RegistrationConflictException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.repository.DeletedAccountStatisticRepository;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerRecoveryService {
    private final PlayerService playerService;
    private final DeletedAccountRepository deletedAccountRepository;
    private final DeletedAccountStatisticRepository deletedAccountStatisticRepository;
    private final PlayerDailyStatisticRepository playerDailyStatisticRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Player recoverAccount(DeletedAccount deletedAccount, String newPassword) {
        if (playerService.existsByLogin(deletedAccount.getLogin()) || playerService.existsByEmail(deletedAccount.getEmail())) {
            throw new RegistrationConflictException("Login or email is already in use by another account");
        }

        boolean isLocal = deletedAccount.getAuthProvider() == PlayerAuthProvider.LOCAL;
        if (isLocal && (newPassword == null || newPassword.isBlank())) {
            throw new IllegalArgumentException("Password is required to recover a local account");
        }

        Optional<DeletedAccountStatistic> statisticOpt =
                deletedAccountStatisticRepository.findByDeletedAccountId(deletedAccount.getId());

        Player player = Player.builder()
                .login(deletedAccount.getLogin())
                .email(deletedAccount.getEmail())
                .role(deletedAccount.getRole())
                .createdAt(deletedAccount.getCreatedAt())
                .lastActiveAt(Instant.now())
                .avatarImage(deletedAccount.getAvatarImage())
                .authProvider(deletedAccount.getAuthProvider())
                .totalSuccessfulGuesses(deletedAccount.getTotalSuccessfulGuesses())
                .emailVerified(true)
                .accountLocked(false)
                .currentStreak(statisticOpt.map(DeletedAccountStatistic::getCurrentStreak).orElse(0))
                .longestStreak(statisticOpt.map(DeletedAccountStatistic::getLongestStreak).orElse(0))
                .currentSuperstreak(statisticOpt.map(DeletedAccountStatistic::getCurrentSuperstreak).orElse(0))
                .longestSuperstreak(statisticOpt.map(DeletedAccountStatistic::getLongestSuperstreak).orElse(0))
                .passwordHash(isLocal ? passwordEncoder.encode(newPassword) : "")
                .build();

        Player savedPlayer = playerService.save(player);

        statisticOpt.ifPresent(statistic -> restoreDailyStatistics(savedPlayer, statistic));
        statisticOpt.ifPresent(deletedAccountStatisticRepository::delete);
        deletedAccountRepository.delete(deletedAccount);

        log.info("Player {} recovered their deleted account.", savedPlayer.getLogin());
        return savedPlayer;
    }

    private void restoreDailyStatistics(Player player, DeletedAccountStatistic statistic) {
        List<DailyStatisticSnapshot> snapshot = statistic.getDailyStatisticsSnapshot();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        List<PlayerDailyStatistic> restored = snapshot.stream()
                .map(entry -> PlayerDailyStatistic.builder()
                        .player(player)
                        .gameMode(entry.gameMode())
                        .puzzleDate(entry.puzzleDate())
                        .solvedAt(entry.solvedAt())
                        .numberOfTries(entry.numberOfTries())
                        .leaderboardPosition(entry.leaderboardPosition())
                        .totalParticipants(entry.totalParticipants())
                        .build())
                .toList();
        playerDailyStatisticRepository.saveAll(restored);
    }
}
