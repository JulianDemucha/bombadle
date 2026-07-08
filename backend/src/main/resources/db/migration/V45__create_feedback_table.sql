CREATE TABLE feedback (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    player_id   BIGINT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);
