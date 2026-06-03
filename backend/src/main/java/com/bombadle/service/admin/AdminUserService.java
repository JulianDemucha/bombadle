package com.bombadle.service.admin;

import com.bombadle.dto.request.AdminUserUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.enums.Role;
import com.bombadle.exception.AdminOperationNotAllowedException;
import com.bombadle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final PlayerRepository playerRepository;
    private final AdminAuditService adminAuditService;

    public void blockUser(long actorId, long targetId) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateAdminAction(actor, target, "block");
        target.setAccountLocked(true);
        playerRepository.save(target);
        adminAuditService.logAction(actorId, "block_user_" + targetId, null);
    }

    public void unblockUser(long actorId, long targetId) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateAdminAction(actor, target, "unblock");
        if (target.getMarkedForDeletionAt() != null) {
            throw new AdminOperationNotAllowedException("Cannot unblock account marked for deletion");
        }
        target.setAccountLocked(false);
        playerRepository.save(target);
        adminAuditService.logAction(actorId, "unblock_user_" + targetId, null);
    }

    public void updateUser(long actorId, long targetId, AdminUserUpdateRequest request) {
        Player actor = getPlayer(actorId);
        Player target = getPlayer(targetId);
        validateAdminAction(actor, target, "update");

        StringBuilder actionType = new StringBuilder("update_user_").append(targetId);
        boolean changed = false;

        if (request.login() != null && !request.login().isBlank()) {
            String login = request.login();
            int length = login.length();
            if (length < 3 || length > 16) {
                throw new IllegalArgumentException("Username must be between 3 and 16 characters");
            }
            String normalizedLogin = login.toLowerCase();
            if (!normalizedLogin.equals(target.getLogin()) && playerRepository.existsByLogin(normalizedLogin)) {
                throw new IllegalArgumentException("Username " + login + " already exists");
            }
            if (!normalizedLogin.equals(target.getLogin())) {
                target.setDisplayName(login);
                target.setLogin(normalizedLogin);
                actionType.append("_change_login_to_").append(login);
                changed = true;
            }
        }

        if (request.avatarImage() != null && !request.avatarImage().isBlank()) {
            AvatarImage avatar = AvatarImage.valueOf(request.avatarImage());
            if (avatar != target.getAvatarImage()) {
                target.setAvatarImage(avatar);
                actionType.append("_change_avatar_to_").append(avatar);
                changed = true;
            }
        }

        if (Boolean.TRUE.equals(request.clearTodayScore())) {
            boolean cleared = clearTodayScore(target);
            if (cleared) {
                actionType.append("_clear_today_score");
                changed = true;
            }
        }

        if (request.totalSuccessfulGuesses() != null) {
            int wins = request.totalSuccessfulGuesses();
            if (wins < 0) {
                throw new IllegalArgumentException("totalSuccessfulGuesses must be >= 0");
            }
            if (wins != target.getTotalSuccessfulGuesses()) {
                target.setTotalSuccessfulGuesses(wins);
                actionType.append("_change_wins_to_").append(wins);
                changed = true;
            }
        }


        if (changed) {
            playerRepository.save(target);
            adminAuditService.logAction(actorId, actionType.toString(), null);
        }
    }

    private boolean clearTodayScore(Player target) {
        if (Boolean.TRUE.equals(target.getHasGuessedToday())) {
            target.setHasGuessedToday(false);
            target.setTodayScore(null);
            int wins = target.getTotalSuccessfulGuesses();
            target.setTotalSuccessfulGuesses(Math.max(0, wins - 1));
            return true;
        }
        return false;
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
}
