package com.bombadle.service.auth.email;

import com.bombadle.config.ApplicationConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private final String TARGET_EMAIL = "target@mail.com";
    private final String FROM_EMAIL = "no-reply@bombadle.com";
    private final String OTP_CODE = "123456";

    @BeforeEach
    void setUp() {
        when(emailConfig.fromAddress()).thenReturn(FROM_EMAIL);
        when(emailConfig.otpExpiration()).thenReturn(Duration.ofMinutes(15));
    }

    @Test
    void sendActivationEmail_buildsAndSendsMessage() {
        emailService.sendActivationEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(TARGET_EMAIL);
        assertThat(sentMessage.getFrom()).isEqualTo(FROM_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Aktywacja konta - Bombadle");
        assertThat(sentMessage.getText()).contains(OTP_CODE).contains("15");
    }

    @Test
    void sendPasswordResetEmail_buildsAndSendsMessage() {
        emailService.sendPasswordResetEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(TARGET_EMAIL);
        assertThat(sentMessage.getFrom()).isEqualTo(FROM_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Reset hasła - Bombadle");
        assertThat(sentMessage.getText()).contains(OTP_CODE).contains("15");
    }

    @Test
    void sendAccountDeletionConfirmationEmail_buildsAndSendsMessage() {
        emailService.sendAccountDeletionConfirmationEmail(TARGET_EMAIL, OTP_CODE);

        verify(emailRateLimitService).enforceRateLimit(TARGET_EMAIL);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(TARGET_EMAIL);
        assertThat(sentMessage.getFrom()).isEqualTo(FROM_EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Potwierdzenie usunięcia konta - Bombadle");
        assertThat(sentMessage.getText()).contains(OTP_CODE).contains("15");
    }

    @Test
    void sendEmailSafely_catchesExceptionAndDoesNotPropagate() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendActivationEmail(TARGET_EMAIL, OTP_CODE));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}