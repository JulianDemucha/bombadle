CREATE TABLE mode_exclusion_history (
    id           BIGSERIAL PRIMARY KEY,
    game_mode    VARCHAR(255) NOT NULL,
    excluded_ids JSONB        NOT NULL DEFAULT '[]'::jsonb,
    CONSTRAINT uc_mode_exclusion_history_game_mode UNIQUE (game_mode)
);
