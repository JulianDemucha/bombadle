TRUNCATE TABLE anonymous_guess_list CASCADE;

ALTER TABLE anonymous_guess_list ADD COLUMN anonymous_session_id UUID;
ALTER TABLE anonymous_guess_list ADD COLUMN game_mode VARCHAR(255);

ALTER TABLE anonymous_guess_list
    ADD CONSTRAINT fk_anonymous_guess_list_session
        FOREIGN KEY (anonymous_session_id) REFERENCES anonymous_session(id);

ALTER TABLE anonymous_session DROP CONSTRAINT IF EXISTS fk_anonymous_session_guess_list_id;
ALTER TABLE anonymous_session DROP COLUMN IF EXISTS anonymous_guess_list_id;