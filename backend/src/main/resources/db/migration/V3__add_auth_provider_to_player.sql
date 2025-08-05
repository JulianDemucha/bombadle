ALTER TABLE player
    ADD COLUMN auth_provider VARCHAR(15);

UPDATE player
SET auth_provider = 'LOCAL'
WHERE auth_provider IS NULL;

ALTER TABLE player
    ALTER COLUMN auth_provider SET NOT NULL;