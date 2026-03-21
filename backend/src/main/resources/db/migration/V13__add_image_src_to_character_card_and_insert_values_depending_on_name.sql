ALTER TABLE character_card
    ADD image_src VARCHAR(255);

UPDATE character_card
SET image_src = '/images/character_cards/' || translate(LOWER(name), 'ąćęłńóśźż ', 'acelnoszz_') || '.jpg';