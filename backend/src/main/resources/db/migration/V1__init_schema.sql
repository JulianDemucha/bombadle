CREATE TABLE character_affiliations
(
    character_id BIGINT       NOT NULL,
    affiliation  VARCHAR(255) NOT NULL
);

CREATE TABLE character_card
(
    id                       BIGINT       NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    alive                    BIT(1) NULL,
    race                     VARCHAR(255) NULL,
    first_appearance_episode INT NULL,
    CONSTRAINT pk_character_card PRIMARY KEY (id)
);

CREATE TABLE player
(
    id                BIGINT       NOT NULL,
    login             VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    `role`            VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    created_at        datetime     NOT NULL,
    last_login_at     datetime NULL,
    today_score_id    BIGINT NULL,
    avatar_image      VARCHAR(255) NULL,
    total_guesses     INT NULL,
    has_guessed_today BIT(1) NULL,
    CONSTRAINT pk_player PRIMARY KEY (id)
);

CREATE TABLE quotes
(
    id                BIGINT        NOT NULL,
    content           VARCHAR(1000) NOT NULL,
    character_card_id BIGINT        NOT NULL,
    CONSTRAINT pk_quotes PRIMARY KEY (id)
);

CREATE TABLE score
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    score_time      time NOT NULL,
    number_of_tries INT  NOT NULL,
    CONSTRAINT pk_score PRIMARY KEY (id)
);

ALTER TABLE player
    ADD CONSTRAINT uc_player_email UNIQUE (email);

ALTER TABLE player
    ADD CONSTRAINT uc_player_login UNIQUE (login);

ALTER TABLE player
    ADD CONSTRAINT uc_player_today_score UNIQUE (today_score_id);

ALTER TABLE player
    ADD CONSTRAINT FK_PLAYER_ON_TODAY_SCORE FOREIGN KEY (today_score_id) REFERENCES score (id);

ALTER TABLE quotes
    ADD CONSTRAINT FK_QUOTES_ON_CHARACTER_CARD FOREIGN KEY (character_card_id) REFERENCES character_card (id);

ALTER TABLE character_affiliations
    ADD CONSTRAINT fk_character_affiliations_on_character_card FOREIGN KEY (character_id) REFERENCES character_card (id);