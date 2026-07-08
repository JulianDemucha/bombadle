package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String LOGO_RESOURCE = "email/logo.png";

    private final JavaMailSender mailSender;
    private final ApplicationConfigProperties.EmailConfig emailConfig;
    private final EmailRateLimitService emailRateLimitService;

    public void sendActivationEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending activation email to: {}", toAddress);

        String html = OtpEmailTemplate.build(
                "Aktywacja konta",
                "Witaj! Dziękujemy za rejestrację. Użyj poniższego kodu, aby aktywować swoje konto.",
                otpCode, getExpirationMinutes(),
                "Jeśli to nie Ty zakładałeś konto, zignoruj tę wiadomość.");

        sendHtmlEmail(toAddress, "Aktywacja konta - Bombadle", html);
    }

    public void sendPasswordResetEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending password reset email to: {}", toAddress);

        String html = OtpEmailTemplate.build(
                "Reset hasła",
                "Otrzymaliśmy prośbę o zmianę hasła. Użyj poniższego kodu, aby ustawić nowe hasło.",
                otpCode, getExpirationMinutes(),
                "Jeśli to nie Ty prosiłeś o reset, zignoruj tę wiadomość.");

        sendHtmlEmail(toAddress, "Reset hasła - Bombadle", html);
    }

    public void sendAccountDeletionConfirmationEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending account deletion confirmation email to: {}", toAddress);

        String html = OtpEmailTemplate.build(
                "Usunięcie konta",
                "Otrzymaliśmy prośbę o trwałe usunięcie Twojego konta. Wpisz poniższy kod, aby potwierdzić usunięcie.",
                otpCode, getExpirationMinutes(),
                "Jeśli to nie Ty prosiłeś o usunięcie konta, zignoruj tę wiadomość i natychmiast zmień swoje hasło.");

        sendHtmlEmail(toAddress, "Potwierdzenie usunięcia konta - Bombadle", html);
    }

    public void sendAccountRecoveryEmail(String toAddress, String otpCode) {
        emailRateLimitService.enforceRateLimit(toAddress);
        log.info("Sending account recovery email to: {}", toAddress);

        String html = OtpEmailTemplate.build(
                "Odzyskiwanie konta",
                "Otrzymaliśmy prośbę o odzyskanie Twojego konta. Użyj poniższego kodu, aby je odzyskać.",
                otpCode, getExpirationMinutes(),
                "Jeśli to nie Ty prosiłeś o odzyskanie konta, zignoruj tę wiadomość.");

        sendHtmlEmail(toAddress, "Odzyskiwanie konta - Bombadle", html);
    }

    private void sendHtmlEmail(String toAddress, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true so the inline logo can be attached alongside the HTML body.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailConfig.fromAddress());
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline(OtpEmailTemplate.LOGO_CID, new ClassPathResource(LOGO_RESOURCE), "image/png");

            mailSender.send(message);
            log.info("Email sent successfully to {}", toAddress);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toAddress, e);
        }
    }

    private long getExpirationMinutes() {
        return emailConfig.otpExpiration().toMinutes();
    }
}
