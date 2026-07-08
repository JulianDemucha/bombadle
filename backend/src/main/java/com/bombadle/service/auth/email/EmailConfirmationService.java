package com.bombadle.service.auth.email;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.AccountRecoveryConfirmRequest;
import com.bombadle.dto.request.PasswordResetRequest;
import com.bombadle.dto.request.VerificationCodeRequest;
import com.bombadle.dto.request.VerificationCodeWithEmailRequest;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.Player;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.service.player.PlayerCredentialsService;
import com.bombadle.service.player.PlayerDeletionService;
import com.bombadle.service.player.PlayerRecoveryService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConfirmationService {


    private final VerificationTokenService verificationTokenService;
    private final AccountRecoveryTokenService accountRecoveryTokenService;
    private final PlayerDeletionService playerDeletionService;
    private final PlayerRecoveryService playerRecoveryService;
    private final PlayerService playerService;
    private final PlayerCredentialsService playerCredentialsService;
    private final DeletedAccountRepository deletedAccountRepository;

    public void confirmEmailVerification(VerificationCodeWithEmailRequest request) {
        Player player = playerService.findByEmail(request.email()).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        verificationTokenService.verifyAndConsume(player.getId(), EmailVerificationType.ACCOUNT_ACTIVATION, request.code());
        playerCredentialsService.activateAccount(player.getId());
        log.info("Account for player: {} has been successfully activated.", player.getEmail());
    }

    public void confirmResetPassword(PasswordResetRequest request) {
        Player player = playerService.findByEmail(request.email()).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        verificationTokenService.verifyAndConsume(player.getId(), EmailVerificationType.PASSWORD_RESET, request.code());
        playerCredentialsService.changePassword(player.getId(), request.newPassword());
        log.info("Password has been successfully reset for player {}", player.getEmail());
    }

    public void confirmPlayerSelfDeletion(VerificationCodeRequest request, PlayerPrincipal playerPrincipal) {
        verificationTokenService.verifyAndConsume(playerPrincipal.getPlayerId(), EmailVerificationType.ACCOUNT_DELETION, request.code());
        playerDeletionService.deletePlayerSelf(playerPrincipal.getPlayerId(), request.deleteAllDataNow());
        log.info("Player {} has been deleted.", playerPrincipal.getUsername());
    }

    public void confirmAccountRecovery(AccountRecoveryConfirmRequest request) {
        DeletedAccount deletedAccount = deletedAccountRepository.findByEmail(request.email().toLowerCase()).orElseThrow(
                () -> new UsernameNotFoundException("Deleted account not found")
        );

        accountRecoveryTokenService.verifyAndConsume(deletedAccount.getId(), EmailVerificationType.ACCOUNT_RECOVERY, request.code());
        playerRecoveryService.recoverAccount(deletedAccount, request.newPassword());
        log.info("Account for {} has been recovered.", deletedAccount.getEmail());
    }

}
