ALTER TABLE character_card
    DROP CONSTRAINT IF EXISTS character_card_gender_check;

ALTER TABLE character_card
    ALTER COLUMN gender TYPE VARCHAR(7),  /* max 7 letters - UNKNOWN (forget the 255 from v5)*/
    ALTER COLUMN gender SET DEFAULT 'OTHER',
    ADD CONSTRAINT character_card_gender_check CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN'));
