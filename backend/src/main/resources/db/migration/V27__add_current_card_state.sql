CREATE TABLE current_card_state
(
    id                    INTEGER NOT NULL CHECK (id = 1),
    current_character_id  BIGINT,
    previous_character_id BIGINT,
    CONSTRAINT pk_current_card_state PRIMARY KEY (id),
    CONSTRAINT fk_current_card_state_on_current_character FOREIGN KEY (current_character_id) REFERENCES character_card (id),
    CONSTRAINT fk_current_card_state_on_previous_character FOREIGN KEY (previous_character_id) REFERENCES character_card (id)
);