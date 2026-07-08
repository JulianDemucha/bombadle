package com.bombadle.service.auth.email;

import com.bombadle.entity.AccountRecoveryToken;
import com.bombadle.enums.EmailVerificationType;
import com.bombadle.exception.ExpiredOtpException;
import com.bombadle.exception.InvalidOtpException;
import com.bombadle.exception.OtpNotFoundException;
import com.bombadle.repository.AccountRecoveryTokenRepository;
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
class AccountRecoveryTokenServiceTest {

    @Mock
    private AccountRecoveryTokenRepository repo;

    @InjectMocks
    private AccountRecoveryTokenService accountRecoveryTokenService;

    @Captor
    private ArgumentCaptor<AccountRecoveryToken> tokenCaptor;

    private final Long DELETED_ACCOUNT_ID = 5L;

    @Nested
    class GenerateNewTokenTests {
        @Test
        void generateNewToken_deletesOldTokenAndSavesNew() {
            AccountRecoveryToken oldToken = AccountRecoveryToken.builder().build();
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.of(oldToken));
            when(repo.save(any(AccountRecoveryToken.class))).thenAnswer(i -> i.getArgument(0));

            AccountRecoveryToken result = accountRecoveryTokenService.generateNewToken(
                    DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, 15);

            verify(repo).delete(oldToken);
            verify(repo).save(tokenCaptor.capture());

            AccountRecoveryToken savedToken = tokenCaptor.getValue();
            assertThat(result).isEqualTo(savedToken);
            assertThat(savedToken.getDeletedAccountId()).isEqualTo(DELETED_ACCOUNT_ID);
            assertThat(savedToken.getEmailVerificationType()).isEqualTo(EmailVerificationType.ACCOUNT_RECOVERY);
            assertThat(savedToken.getVerificationCode()).hasSize(6).matches("\\d+");
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        void generateNewToken_noOldToken_justSavesNew() {
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.empty());

            accountRecoveryTokenService.generateNewToken(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, 15);

            verify(repo, never()).delete(any());
            verify(repo).save(any(AccountRecoveryToken.class));
        }
    }

    @Nested
    class VerifyAndConsumeTests {
        @Test
        void verifyAndConsume_validCode_deletesToken() {
            AccountRecoveryToken token = AccountRecoveryToken.builder()
                    .verificationCode("123456")
                    .expiresAt(Instant.now().plusSeconds(600))
                    .build();
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.of(token));

            accountRecoveryTokenService.verifyAndConsume(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, "123456");

            verify(repo).delete(token);
        }

        @Test
        void verifyAndConsume_tokenNotFound_throwsOtpNotFoundException() {
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.empty());

            assertThrows(OtpNotFoundException.class,
                    () -> accountRecoveryTokenService.verifyAndConsume(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, "123456"));
            verify(repo, never()).delete(any());
        }

        @Test
        void verifyAndConsume_invalidCode_throwsInvalidOtpException() {
            AccountRecoveryToken token = AccountRecoveryToken.builder()
                    .verificationCode("654321")
                    .build();
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.of(token));

            assertThrows(InvalidOtpException.class,
                    () -> accountRecoveryTokenService.verifyAndConsume(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, "123456"));
            verify(repo, never()).delete(any());
        }

        @Test
        void verifyAndConsume_expiredToken_deletesTokenAndThrowsExpiredOtpException() {
            AccountRecoveryToken token = AccountRecoveryToken.builder()
                    .verificationCode("123456")
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();
            when(repo.findByDeletedAccountIdAndEmailVerificationType(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY))
                    .thenReturn(Optional.of(token));

            assertThrows(ExpiredOtpException.class,
                    () -> accountRecoveryTokenService.verifyAndConsume(DELETED_ACCOUNT_ID, EmailVerificationType.ACCOUNT_RECOVERY, "123456"));
            verify(repo).delete(token);
        }
    }
}
