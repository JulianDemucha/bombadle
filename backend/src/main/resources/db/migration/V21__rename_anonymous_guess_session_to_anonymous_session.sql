CREATE SEQUENCE IF NOT EXISTS anonymous_session_seq START WITH 1 INCREMENT BY 50;

ALTER TABLE anonymous_guess_session
    RENAME TO anonymous_session;