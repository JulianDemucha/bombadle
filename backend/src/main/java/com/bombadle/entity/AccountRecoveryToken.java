package com.bombadle.entity;

import com.bombadle.enums.EmailVerificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_recovery_token")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class AccountRecoveryToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "deleted_account_id", nullable = false)
    private Long deletedAccountId;

    @Column(name = "email_verification_type")
    private EmailVerificationType emailVerificationType;

    @Column(name = "expires_at")
    private Instant expiresAt;

}
