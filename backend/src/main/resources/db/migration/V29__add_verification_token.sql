CREATE SEQUENCE IF NOT EXISTS verification_token_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE verification_token
(
    id                      BIGINT NOT NULL,
    verification_code       VARCHAR(255),
    player_id               BIGINT,
    email_verification_type SMALLINT,
    expires_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_verificationtoken PRIMARY KEY (id)
);