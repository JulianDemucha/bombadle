TRUNCATE TABLE anonymous_guess_list CASCADE;

ALTER TABLE quote RENAME COLUMN content TO quote_beginning;
ALTER TABLE quote ALTER COLUMN quote_beginning TYPE VARCHAR(1000);

ALTER TABLE quote ADD COLUMN correct_answer VARCHAR(255) NOT NULL;

CREATE TABLE quote_options (
                               quote_id BIGINT NOT NULL,
                               option_text VARCHAR(255) NOT NULL
);

ALTER TABLE quote_options
    ADD CONSTRAINT fk_quote_options_on_quote
        FOREIGN KEY (quote_id) REFERENCES quote (id);

ALTER TABLE anonymous_guess_list ADD COLUMN anonymous_session_id UUID;
ALTER TABLE anonymous_guess_list ADD COLUMN game_mode VARCHAR(255);

ALTER TABLE anonymous_guess_list ALTER COLUMN anonymous_session_id SET NOT NULL;
ALTER TABLE anonymous_guess_list ALTER COLUMN game_mode SET NOT NULL;

ALTER TABLE anonymous_guess_list
    ADD CONSTRAINT fk_anonymous_guess_list_session
        FOREIGN KEY (anonymous_session_id) REFERENCES anonymous_session(id);

ALTER TABLE anonymous_session DROP CONSTRAINT IF EXISTS fk_anonymous_session_guess_list_id;
ALTER TABLE anonymous_session DROP COLUMN IF EXISTS anonymous_guess_list_id;

ALTER TABLE current_card_state ADD current_quote_id BIGINT;
ALTER TABLE current_card_state ADD previous_quote_id BIGINT;

ALTER TABLE current_card_state
    ADD CONSTRAINT fk_current_card_state_on_current_quote
        FOREIGN KEY (current_quote_id) REFERENCES quote (id);

ALTER TABLE current_card_state
    ADD CONSTRAINT fk_current_card_state_on_previous_quote
        FOREIGN KEY (previous_quote_id) REFERENCES quote (id);