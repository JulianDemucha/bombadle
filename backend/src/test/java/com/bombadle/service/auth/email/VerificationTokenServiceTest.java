package com.bombadle.service.auth.email;

import com.bombadle.entity.Player;
import com.bombadle.entity.VerificationToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.exception.ExpiredOtpException;
import com.bombadle.exception.InvalidOtpException;
import com.bombadle.exception.OtpNotFoundException;
import com.bombadle.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository repo;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    @Captor
    private ArgumentCaptor<VerificationToken> tokenCaptor;

    private Player player;
    private final Long PLAYER_ID = 1L;

    @BeforeEach
    void setUp() {
        player = Player.builder().id(PLAYER_ID).build();
    }

    @Nested
    class GenerateNewTokenTests {
        @Test
        void generateNewToken_deletesOldTokenAndSavesNew() {
            VerificationToken oldToken = VerificationToken.builder().build();
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.of(oldToken));
            when(repo.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            VerificationToken result = verificationTokenService.generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15);

            verify(repo).delete(oldToken);
            verify(repo).save(tokenCaptor.capture());

            VerificationToken savedToken = tokenCaptor.getValue();
            assertThat(result).isEqualTo(savedToken);
            assertThat(savedToken.getPlayer()).isEqualTo(player);
            assertThat(savedToken.getEmailVerificationType()).isEqualTo(EmailVerificationType.ACCOUNT_ACTIVATION);
            assertThat(savedToken.getVerificationCode()).hasSize(6).matches("\\d+");
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        void generateNewToken_noOldToken_justSavesNew() {
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.empty());

            verificationTokenService.generateNewToken(player, EmailVerificationType.ACCOUNT_ACTIVATION, 15);

            verify(repo, never()).delete(any());
            verify(repo).save(any(VerificationToken.class));
        }
    }

    @Nested
    class VerifyAndConsumeTests {
        @Test
        void verifyAndConsume_validCode_deletesToken() {
            VerificationToken token = VerificationToken.builder()
                    .verificationCode("123456")
                    .expiresAt(Instant.now().plusSeconds(600))
                    .build();
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.of(token));

            verificationTokenService.verifyAndConsume(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION, "123456");

            verify(repo).delete(token);
        }

        @Test
        void verifyAndConsume_tokenNotFound_throwsOtpNotFoundException() {
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.empty());

            assertThrows(OtpNotFoundException.class,
                    () -> verificationTokenService.verifyAndConsume(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION, "123456"));
            verify(repo, never()).delete(any());
        }

        @Test
        void verifyAndConsume_invalidCode_throwsInvalidOtpException() {
            VerificationToken token = VerificationToken.builder()
                    .verificationCode("654321")
                    .build();
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.of(token));

            assertThrows(InvalidOtpException.class,
                    () -> verificationTokenService.verifyAndConsume(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION, "123456"));
            verify(repo, never()).delete(any());
        }

        @Test
        void verifyAndConsume_expiredToken_deletesTokenAndThrowsExpiredOtpException() {
            VerificationToken token = VerificationToken.builder()
                    .verificationCode("123456")
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();
            when(repo.findByPlayerIdAndEmailVerificationType(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION))
                    .thenReturn(Optional.of(token));

            assertThrows(ExpiredOtpException.class,
                    () -> verificationTokenService.verifyAndConsume(PLAYER_ID, EmailVerificationType.ACCOUNT_ACTIVATION, "123456"));
            verify(repo).delete(token);
        }
    }
}