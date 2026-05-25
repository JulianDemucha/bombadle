package com.bombadle.service.admin;

import com.bombadle.entity.Player;
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
        target.setAccountLocked(false);
        playerRepository.save(target);
        adminAuditService.logAction(actorId, "unblock_user_" + targetId, null);
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

