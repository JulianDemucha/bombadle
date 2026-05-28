ALTER TABLE anonymous_session
    ADD last_active_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE anonymous_session
    ALTER COLUMN last_active_at SET NOT NULL;

ALTER TABLE player
    RENAME COLUMN last_login_at TO last_active_at;