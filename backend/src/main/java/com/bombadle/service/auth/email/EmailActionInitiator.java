package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.entity.AccountRecoveryToken;
import com.bombadle.entity.DeletedAccount;
import com.bombadle.entity.Player;
import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.repository.DeletedAccountRepository;
import com.bombadle.service.player.PlayerCredentialsService;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailActionInitiator {

    private final VerificationTokenService tokenService;
    private final AccountRecoveryTokenService accountRecoveryTokenService;
    private final EmailService emailService;
    private final ApplicationConfigProperties.EmailConfig emailConfig;
    private final PlayerService playerService;
    private final PlayerCredentialsService playerCredentialsService;
    private final DeletedAccountRepository deletedAccountRepository;

    @Async
    @Transactional
    public void initiateAccountActivation(Player player) {
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.ACCOUNT_ACTIVATION, getExpirationMinutes());

        emailService.sendActivationEmail(player.getEmail(), token.getVerificationCode());
        playerCredentialsService.recordEmailSent(player.getId());
        log.info("Account activation process initiated for: {}", player.getEmail());
    }

    @Async
    @Transactional
    public void initiateAccountActivation(String email) {
        Player player = playerService.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        initiateAccountActivation(player);
    }

    @Async
    @Transactional
    public void initiatePasswordReset(String email) {
        Player player = playerService.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.PASSWORD_RESET, getExpirationMinutes());

        emailService.sendPasswordResetEmail(player.getEmail(), token.getVerificationCode());
        playerCredentialsService.recordEmailSent(player.getId());
        log.info("Password reset process initiated for: {}", player.getEmail());
    }

    @Async
    @Transactional
    public void initiateAccountDeletion(Player player) {
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.ACCOUNT_DELETION, getExpirationMinutes());

        emailService.sendAccountDeletionConfirmationEmail(player.getEmail(), token.getVerificationCode());
        playerCredentialsService.recordEmailSent(player.getId());
        log.info("Account deletion process initiated for: {}", player.getEmail());
    }

    @Async
    @Transactional
    public void initiateAccountRecovery(String email) {
        DeletedAccount deletedAccount = deletedAccountRepository.findByEmail(email.toLowerCase()).orElseThrow(
                () -> new UsernameNotFoundException("Deleted account not found")
        );

        AccountRecoveryToken token = accountRecoveryTokenService.generateNewToken(
                deletedAccount.getId(), EmailVerificationType.ACCOUNT_RECOVERY, getExpirationMinutes());

        emailService.sendAccountRecoveryEmail(deletedAccount.getEmail(), token.getVerificationCode());
        log.info("Account recovery process initiated for: {}", deletedAccount.getEmail());
    }

    private int getExpirationMinutes(){
        return (int) emailConfig.otpExpiration().toMinutes();
    }
}