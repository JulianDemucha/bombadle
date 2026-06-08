package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ApplicationConfigProperties.EmailConfig emailConfig;
    private final EmailRateLimitService emailRateLimitService;

    public void sendActivationEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending activation email to: {}", toAddress);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailConfig.fromAddress());
        message.setTo(toAddress);
        message.setSubject("Aktywacja konta - Bombadle");

        message.setText("Witaj!\n\nTwój kod aktywacyjny to: " + otpCode +
                "\n\nKod jest ważny przez " + getExpirationMinutes() + " minut.");

        sendEmailSafely(message, toAddress);
    }

    public void sendPasswordResetEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending password reset email to: {}", toAddress);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailConfig.fromAddress());
        message.setTo(toAddress);
        message.setSubject("Reset hasła - Bombadle");

        message.setText("Otrzymaliśmy prośbę o zmianę hasła.\n\n" +
                "Twój kod do zresetowania hasła to: " + otpCode +
                "\n\nKod jest ważny przez " + getExpirationMinutes() + " minut. " +
                "Jeśli to nie Ty prosiłeś o reset, zignoruj tę wiadomość.");

        sendEmailSafely(message, toAddress);
    }

    public void sendAccountDeletionConfirmationEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending account deletion confirmation email to: {}", toAddress);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailConfig.fromAddress());
        message.setTo(toAddress);
        message.setSubject("Potwierdzenie usunięcia konta - Bombadle");

        message.setText("Otrzymaliśmy prośbę o trwałe usunięcie Twojego konta.\n\n" +
                "Aby potwierdzić usunięcie, wpisz poniższy kod w aplikacji:\n" +
                otpCode +
                "\n\nKod jest ważny przez " + getExpirationMinutes() + " minut. " +
                "Jeśli to nie Ty prosiłeś o usunięcie konta, zignoruj tę wiadomość i natychmiast zmień swoje hasło.");

        sendEmailSafely(message, toAddress);
    }

    private void sendEmailSafely(SimpleMailMessage message, String toAddress) {
        try {
            mailSender.send(message);
            log.info("Email sent successfully to {}", toAddress);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toAddress, e);
        }
    }

    private long getExpirationMinutes(){
        return emailConfig.otpExpiration().toMinutes();
    }
}