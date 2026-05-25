package com.bombadle.service.player;

import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.Player;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.repository.GuessListRepository;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.repository.RefreshTokenRepository;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.admin.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerDeletionService {
    private static final Logger log = LoggerFactory.getLogger(PlayerDeletionService.class);

    private final PlayerRepository playerRepository;
    private final AdminAuditService adminAuditService;
    private final GuessListRepository guessListRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ScoreRepository scoreRepository;
    private final DeletedAccountRepository deletedAccountRepository;

    public void markForDeletion(long actorId, long targetId) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateAdminAction(actor, target, "mark for deletion");
        target.setMarkedForDeletionAt(Instant.now());
        target.setAccountLocked(true);
        playerRepository.save(target);
        adminAuditService.logAction(actorId, "mark_user_" + targetId + "_for_deletion", null);
    }

    public void cancelDeletion(long actorId, long targetId) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateAdminAction(actor, target, "cancel deletion");
        target.setMarkedForDeletionAt(null);
        target.setAccountLocked(false);
        playerRepository.save(target);
        adminAuditService.logAction(actorId, "cancel_mark_user_" + targetId, null);
    }

    public void deleteMarkedForDeletion(Duration retention) {
        Instant cutoff = Instant.now().minus(retention);
        List<Player> targets = playerRepository.findAllByMarkedForDeletionAtBefore(cutoff);
        for (Player target : targets) {
            log.info("Auto delete marked user: {}", target.getId());
            deletePlayerWithSnapshotNoAudit(target, null);
        }
    }

    public void deletePlayerSelf(long playerId) {
        Player target = getPlayer(playerId);
        deletePlayerWithSnapshot(target, playerId, "delete_user_self_" + target.getId());
    }

    public void deletePlayerByAdmin(long actorId, long targetId) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateSuperadminDelete(actor, target);
        deletePlayerWithSnapshot(target, actorId, "delete_user_" + targetId);
    }

    public void deletePlayerWithSnapshot(Player target, long actorId, String actionType) {
        snapshotDeletedAccount(target, actorId);
        guessListRepository.deleteByPlayerId(target.getId());
        refreshTokenRepository.deleteByPlayerId(target.getId());
        scoreRepository.deleteByPlayerId(target.getId());
        playerRepository.delete(target);
        adminAuditService.logAction(actorId, actionType, null);
    }

    private void deletePlayerWithSnapshotNoAudit(Player target, Long actorId) {
        snapshotDeletedAccount(target, actorId);
        guessListRepository.deleteByPlayerId(target.getId());
        refreshTokenRepository.deleteByPlayerId(target.getId());
        scoreRepository.deleteByPlayerId(target.getId());
        playerRepository.delete(target);
    }

    private void snapshotDeletedAccount(Player target, Long actorId) {
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
        deletedAccountRepository.save(snapshot);
    }

    private Player getPlayer(long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
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
