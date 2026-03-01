ALTER TABLE player
    ALTER COLUMN auth_provider TYPE VARCHAR(255) USING (auth_provider::VARCHAR(255));

ALTER TABLE character_card
    ALTER COLUMN gender TYPE VARCHAR(255) USING (gender::VARCHAR(255));