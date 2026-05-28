ALTER TABLE anonymous_session
    RENAME COLUMN is_guessed TO has_guessed_today;

DROP SEQUENCE IF EXISTS anonymous_session_seq CASCADE; /* i created it for id to be long, but i switched back to uuid */