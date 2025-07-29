ALTER TABLE character_card
    ADD COLUMN gender VARCHAR(6) /* max 6 letters - female */
        NOT NULL
        DEFAULT 'other'
        CHECK (gender IN ('male','female','other'));