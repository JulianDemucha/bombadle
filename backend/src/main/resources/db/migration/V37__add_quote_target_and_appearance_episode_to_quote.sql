ALTER TABLE quote
    ADD appearance_episode INTEGER;

ALTER TABLE quote
    ADD quote_target VARCHAR(255);

ALTER TABLE quote
    ALTER COLUMN appearance_episode SET NOT NULL;

ALTER TABLE quote
    ALTER COLUMN quote_target SET NOT NULL;