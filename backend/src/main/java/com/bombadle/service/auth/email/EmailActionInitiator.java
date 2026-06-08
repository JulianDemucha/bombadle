package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import com.bombadle.entity.Player;
import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailActionInitiator {

    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final ApplicationConfigProperties.EmailConfig emailConfig;
    private final PlayerService playerService;

    public void initiateAccountActivation(Player player) {
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.ACCOUNT_ACTIVATION, getExpirationMinutes());

        emailService.sendActivationEmail(player.getEmail(), token.getVerificationCode());
        log.info("Account activation process initiated for: {}", player.getEmail());
    }

    public void initiatePasswordReset(String email) {
        Player player = playerService.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.PASSWORD_RESET, getExpirationMinutes());

        emailService.sendPasswordResetEmail(player.getEmail(), token.getVerificationCode());
        log.info("Password reset process initiated for: {}", player.getEmail());
    }

    public void initiateAccountDeletion(Player player) {
        VerificationToken token = tokenService.generateNewToken(
                player, EmailVerificationType.ACCOUNT_DELETION, getExpirationMinutes());

        emailService.sendAccountDeletionConfirmationEmail(player.getEmail(), token.getVerificationCode());
        log.info("Account deletion process initiated for: {}", player.getEmail());
    }

    private int getExpirationMinutes(){
        return (int) emailConfig.otpExpiration().toMinutes();
    }
}