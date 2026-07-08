CREATE SEQUENCE IF NOT EXISTS account_recovery_token_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE account_recovery_token
(
    id                      BIGINT NOT NULL,
    verification_code       VARCHAR(255),
    deleted_account_id      BIGINT NOT NULL,
    email_verification_type SMALLINT,
    expires_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_account_recovery_token PRIMARY KEY (id)
);

CREATE INDEX idx_account_recovery_token_deleted_account_id ON account_recovery_token (deleted_account_id);
