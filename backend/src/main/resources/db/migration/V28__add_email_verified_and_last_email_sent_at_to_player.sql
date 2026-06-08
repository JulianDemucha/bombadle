ALTER TABLE player
    ADD email_verified BOOLEAN;

ALTER TABLE player
    ADD last_email_sent_at TIMESTAMP WITHOUT TIME ZONE;
