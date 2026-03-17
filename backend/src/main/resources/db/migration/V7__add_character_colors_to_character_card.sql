CREATE TABLE character_colors
(
    character_id BIGINT   NOT NULL,
    colors       VARCHAR(255) NOT NULL
);

ALTER TABLE character_colors
    ADD CONSTRAINT fk_character_colors_on_character_card FOREIGN KEY (character_id) REFERENCES character_card (id);