ALTER TABLE player
    ALTER COLUMN avatar_image SET NOT NULL;

ALTER TABLE player
    ALTER COLUMN has_guessed_today SET NOT NULL;

ALTER TABLE player
    ALTER COLUMN last_login_at SET NOT NULL;

ALTER TABLE player
    ALTER COLUMN total_guesses SET NOT NULL;