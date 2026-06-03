ALTER TABLE player ADD COLUMN display_name VARCHAR(16);

UPDATE player SET display_name = login WHERE display_name IS NULL;

UPDATE player SET login = LOWER(login);
UPDATE player SET email = LOWER(email);

ALTER TABLE player ADD CONSTRAINT uk_player_login UNIQUE (login);
ALTER TABLE player ADD CONSTRAINT uk_player_email UNIQUE (email);

