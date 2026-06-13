CREATE TABLE current_card_mapping
(
    state_id     INTEGER      NOT NULL,
    character_id BIGINT       NOT NULL,
    game_mode    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_current_card_mapping PRIMARY KEY (state_id, game_mode),
    CONSTRAINT fk_current_mapping_state FOREIGN KEY (state_id) REFERENCES current_card_state (id),
    CONSTRAINT fk_current_mapping_char FOREIGN KEY (character_id) REFERENCES character_card (id)
);

CREATE TABLE previous_card_mapping
(
    state_id     INTEGER      NOT NULL,
    character_id BIGINT       NOT NULL,
    game_mode    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_previous_card_mapping PRIMARY KEY (state_id, game_mode),
    CONSTRAINT fk_previous_mapping_state FOREIGN KEY (state_id) REFERENCES current_card_state (id),
    CONSTRAINT fk_previous_mapping_char FOREIGN KEY (character_id) REFERENCES character_card (id)
);
INSERT INTO current_card_mapping (state_id, character_id, game_mode)
SELECT id, current_character_id, 'CLASSIC'
FROM current_card_state
WHERE current_character_id IS NOT NULL;

INSERT INTO previous_card_mapping (state_id, character_id, game_mode)
SELECT id, previous_character_id, 'CLASSIC'
FROM current_card_state
WHERE previous_character_id IS NOT NULL;

ALTER TABLE current_card_state DROP CONSTRAINT fk_current_card_state_on_current_character;
ALTER TABLE current_card_state DROP CONSTRAINT fk_current_card_state_on_previous_character;

ALTER TABLE current_card_state DROP COLUMN current_character_id;
ALTER TABLE current_card_state DROP COLUMN previous_character_id;