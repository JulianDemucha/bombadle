package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ApplicationConfigProperties.EmailConfig emailConfig;

    @Mock
    private EmailRateLimitService emailRateLimitService;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private final String TARGET_EMAIL = "target@mail.com";
    private final String FROM_EMAIL = "no-reply@bombadle.com";
    private final String OTP_CODE = "123456";

    @BeforeEach
    void setUp() {
        when(emailConfig.fromAddress()).thenReturn(FROM_EMAIL);
        when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
        // Real MimeMessage so the helper can populate headers/body.
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
    }

    @Test
    void sendActivationEmail_buildsAndSendsMessage() throws Exception {
        emailService.sendActivationEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sent = messageCaptor.getValue();
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(TARGET_EMAIL);
        assertThat(sent.getFrom()[0].toString()).isEqualTo(FROM_EMAIL);
        assertThat(sent.getSubject()).isEqualTo("Aktywacja konta - Bombadle");
    }

    @Test
    void sendPasswordResetEmail_buildsAndSendsMessage() throws Exception {
        emailService.sendPasswordResetEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sent = messageCaptor.getValue();
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo(TARGET_EMAIL);
        assertThat(sent.getSubject()).isEqualTo("Reset hasła - Bombadle");
    }

    @Test
    void sendAccountDeletionConfirmationEmail_buildsAndSendsMessage() throws Exception {
        emailService.sendAccountDeletionConfirmationEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sent = messageCaptor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Potwierdzenie usunięcia konta - Bombadle");
    }

    @Test
    void sendEmailSafely_catchesExceptionAndDoesNotPropagate() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendActivationEmail(TARGET_EMAIL, OTP_CODE));

        verify(mailSender).send(any(MimeMessage.class));
    }
}
