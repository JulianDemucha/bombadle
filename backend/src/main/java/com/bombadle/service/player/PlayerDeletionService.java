package com.bombadle.service.player;

import com.bombadle.dto.DailyStatisticSnapshot;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.DeletedAccountStatistic;
import com.bombadle.entity.Player;
import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.repository.DeletedAccountStatisticRepository;
import com.bombadle.repository.PlayerDailyStatisticRepository;
import com.bombadle.service.admin.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Snapshots deleted players into DeletedAccount, and logs AdminAudit for admin-only methods/actions

@Service
@RequiredArgsConstructor
public class PlayerDeletionService {
    private static final Logger log = LoggerFactory.getLogger(PlayerDeletionService.class);

    private final AdminAuditService adminAuditService;
    private final DeletedAccountRepository deletedAccountRepository;
    private final DeletedAccountStatisticRepository deletedAccountStatisticRepository;
    private final PlayerDailyStatisticRepository playerDailyStatisticRepository;
    private final PlayerService playerService;
    private final PlayerCascadeDeletionService playerCascadeDeletionService;

    public void markForDeletion(long actorId, long targetId) {
        Player actor = playerService.getPlayerById(actorId);
        Player target = playerService.getPlayerById(targetId);
        validateAdminAction(actor, target, "mark for deletion");
        target.setMarkedForDeletionAt(Instant.now());
        target.setAccountLocked(true);
        playerService.save(target);
        adminAuditService.logAction(actorId, "mark_user_" + targetId + "_for_deletion", null);
    }

    public void cancelDeletion(long actorId, long targetId) {
        Player actor = playerService.getPlayerById(actorId);
        Player target = playerService.getPlayerById(targetId);
        validateAdminAction(actor, target, "cancel deletion");
        target.setMarkedForDeletionAt(null);
        target.setAccountLocked(false);
        playerService.save(target);
        adminAuditService.logAction(actorId, "cancel_mark_user_" + targetId, null);
    }

    public void deleteMarkedForDeletion(Duration retention) {
        Instant cutoff = Instant.now().minus(retention);
        List<Player> targets = playerService.findAllByMarkedForDeletionAtBefore(cutoff);
        for (Player target : targets) {
            log.info("Auto delete marked user: {}", target.getId());
            deletePlayerWithSnapshotNoAudit(target, null);
        }
    }

    @Transactional
    public void purgeExpiredDeletedAccountSnapshots(Duration retention) {
        Instant cutoff = Instant.now().minus(retention);
        List<DeletedAccount> expired = deletedAccountRepository.findAllByDeletedAtBefore(cutoff);
        if (expired.isEmpty()) {
            return;
        }
        List<Long> deletedAccountIds = expired.stream().map(DeletedAccount::getId).toList();
        deletedAccountStatisticRepository.deleteAllByDeletedAccountIdIn(deletedAccountIds);
        deletedAccountRepository.deleteAll(expired);
        log.info("Purged {} expired deleted-account snapshot(s).", expired.size());
    }

    @Transactional
    public void deletePlayerSelf(long playerId, boolean deleteAllDataNow) {
        Player target = playerService.getPlayerById(playerId);
        if (deleteAllDataNow) {
            playerCascadeDeletionService.deletePlayerWithCascade(target);
            return;
        }
        DeletedAccount snapshot = snapshotDeletedAccount(target, playerId);
        snapshotDeletedAccountStatistic(target, snapshot.getId());
        playerCascadeDeletionService.deletePlayerWithCascade(target);
    }

    public void deletePlayerByAdmin(long actorId, long targetId) {
        Player actor = playerService.getPlayerById(actorId);
        Player target = playerService.getPlayerById(targetId);
        validateSuperadminDelete(actor, target);
        deletePlayerWithSnapshot(target, actorId, "delete_user_" + targetId);
    }

    public void deletePlayerWithSnapshot(Player target, long actorId, String actionType) {
        snapshotDeletedAccount(target, actorId);
        playerCascadeDeletionService.deletePlayerWithCascade(target);
        adminAuditService.logAction(actorId, actionType, null);
    }

    private void deletePlayerWithSnapshotNoAudit(Player target, Long actorId) {
        snapshotDeletedAccount(target, actorId);
        playerCascadeDeletionService.deletePlayerWithCascade(target);
    }

    private DeletedAccount snapshotDeletedAccount(Player target, Long actorId) {
        DeletedAccount snapshot = DeletedAccount.builder()
                .originalPlayerId(target.getId())
                .login(target.getLogin())
                .email(target.getEmail())
                .role(target.getRole())
                .createdAt(target.getCreatedAt())
                .totalSuccessfulGuesses(target.getTotalSuccessfulGuesses())
                .avatarImage(target.getAvatarImage())
                .authProvider(target.getAuthProvider())
                .deletedAt(Instant.now())
                .deletedByActorId(actorId)
                .build();
        return deletedAccountRepository.save(snapshot);
    }

    private void snapshotDeletedAccountStatistic(Player target, Long deletedAccountId) {
        List<PlayerDailyStatistic> dailyStatistics = playerDailyStatisticRepository.findAllByPlayerId(target.getId());

        Map<GameMode, Integer> guessesByMode = new HashMap<>();
        int totalTop3Finishes = 0;
        List<DailyStatisticSnapshot> dailyStatisticsSnapshot = new ArrayList<>();
        for (PlayerDailyStatistic stat : dailyStatistics) {
            guessesByMode.merge(stat.getGameMode(), 1, Integer::sum);
            if (stat.getLeaderboardPosition() <= 3) {
                totalTop3Finishes++;
            }
            dailyStatisticsSnapshot.add(new DailyStatisticSnapshot(
                    stat.getGameMode(),
                    stat.getPuzzleDate(),
                    stat.getSolvedAt(),
                    stat.getNumberOfTries(),
                    stat.getLeaderboardPosition(),
                    stat.getTotalParticipants()
            ));
        }

        DeletedAccountStatistic statistic = DeletedAccountStatistic.builder()
                .deletedAccountId(deletedAccountId)
                .currentStreak(target.getCurrentStreak())
                .longestStreak(target.getLongestStreak())
                .currentSuperstreak(target.getCurrentSuperstreak())
                .longestSuperstreak(target.getLongestSuperstreak())
                .totalSuccessfulGuesses(target.getTotalSuccessfulGuesses())
                .guessesByMode(guessesByMode)
                .totalTop3Finishes(totalTop3Finishes)
                .dailyStatisticsSnapshot(dailyStatisticsSnapshot)
                .capturedAt(Instant.now())
                .build();
        deletedAccountStatisticRepository.save(statistic);
    }

    private void validateAdminAction(Player actor, Player target, String action) {
        if (actor.getId().equals(target.getId())) {
            throw new AdminOperationNotAllowedException("Cannot " + action + " own account");
        }
        if (actor.getRole() == Role.ROLE_ADMIN && target.getRole() != Role.ROLE_USER) {
            throw new AccessDeniedException("Admins cannot modify other admins or superadmins");
        }
    }

    private void validateSuperadminDelete(Player actor, Player target) {
        if (actor.getId().equals(target.getId())) {
            throw new AdminOperationNotAllowedException("Cannot delete own account");
        }
        if (actor.getRole() != Role.ROLE_SUPERADMIN) {
            throw new AccessDeniedException("Only superadmin can delete accounts directly");
        }
        if (target.getRole() == Role.ROLE_SUPERADMIN) {
            throw new AccessDeniedException("Cannot delete another superadmin account");
        }
    }
}
