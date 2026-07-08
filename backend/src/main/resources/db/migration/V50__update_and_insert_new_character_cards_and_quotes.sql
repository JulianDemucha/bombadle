-- EXISTING CARDS UPDATE
UPDATE character_card SET name = 'Tytus Bogdan Bomba' WHERE id = 1;
UPDATE character_card SET race = 'Nieznany' WHERE id = 4;
UPDATE character_card SET name = 'Michał Głuś' WHERE id = 5;
UPDATE character_card SET name = 'Sułtan Kosmitów' WHERE id = 6;
UPDATE character_card SET name = 'Pani Sraciatello' WHERE id = 11;

-- 2. NEW AFFILIATIONS FOR EXISTING CARDS
INSERT INTO character_affiliations (character_id, affiliation) VALUES
                                                                   (2, 'Boguslaw_Lecina_Uslugi_Budowlano_Remontowe'),
                                                                   (5, 'Sultanat_Kosmitow');

-- NEW ALIASES FOR EXISTING CARDS
INSERT INTO aliases (character_id, aliases) VALUES
                                                (1, 'Kapitan Bomba'), (1, 'Kapitan Dupa'),
                                                (2, 'Sebek'), (2, 'Bonawentura'),
                                                (3, 'Janusz z domu Sram'), (3, 'Janusz kazimierz sram'),
                                                (5, 'Chorąży Głuś'), (5, 'Luigi'), (5, 'Łukasz'), (5, 'Mikele'),
                                                (6, 'Sułtan Kosmitów'), (6, 'Otyły pan'), (6, 'Giovanni Marcello Roberto Sraciatello-Parchaś'),
                                                (7, 'Chorąży Torpeda'), (7, 'Torpedał'), (7, 'Jędrzej „Torpeda” Klemens'),
                                                (8, 'Barbarian'),
                                                (11, 'Matka Sułtana Kosmitów'),
                                                (13, 'El Presidente Sztygar Mkbewe de domo');

-- NEW CARDS INSERT (ID: 16 - 22)
INSERT INTO character_card (id, name, gender, race, alive, first_appearance_episode, image_src) VALUES
                                                                                                    (16, 'Dominik Waldemar Jachaś', 'MALE', 'Kurvinox', TRUE, 88, '/images/character_cards/16.jpg'),
                                                                                                    (17, 'Bogusław Mariusz Łęcina', 'MALE', 'Kurvinox', TRUE, 68, '/images/character_cards/17.jpg'),
                                                                                                    (18, 'Wacław Marsz', 'MALE', 'Czlowiek', TRUE, 68, '/images/character_cards/18.jpg'),
                                                                                                    (19, 'Marcin Hobuca', 'MALE', 'Kutanoid', FALSE, 68, '/images/character_cards/19.jpg'),
                                                                                                    (20, 'Podróżnik Mateo', 'MALE', 'Dodupyzaur', TRUE, 82, '/images/character_cards/20.jpg'),
                                                                                                    (21, 'Cyganka Dagmara', 'FEMALE', 'Kurvinox', TRUE, 103, '/images/character_cards/21.jpg'),
                                                                                                    (22, 'Krol Kurvinoxow', 'MALE', 'Kurvinox', FALSE, 17, '/images/character_cards/22.jpg');

INSERT INTO character_colors (character_id, colors) VALUES
                                                        (16, 'JASNONIEBIESKI'),
                                                        (17, 'NIEBIESKI'),
                                                        (18, 'SZARY'), (18, 'BRAZOWY'),
                                                        (19, 'CZERWONY'),
                                                        (20, 'CZARNY'),
                                                        (21, 'NIEBIESKI'),
                                                        (22, 'ZOLTY');

INSERT INTO character_affiliations (character_id, affiliation) VALUES
                                                                   (16, 'Kosmici'), (16, 'Gwiezdna_Flota'), (16, 'Admiralowie_Gwiezdnej_Floty'),
                                                                   (17, 'Cywile'), (17, 'Kosmici'), (17, 'Boguslaw_Lecina_Uslugi_Budowlano_Remontowe'),
                                                                   (18, 'Cywile'), (18, 'Boguslaw_Lecina_Uslugi_Budowlano_Remontowe'),
                                                                   (19, 'Sultanat_Kosmitow'),
                                                                   (20, 'Kosmici'), (20, 'Cywile'),
                                                                   (21, 'Sultanat_Kosmitow'), (21, 'Kosmici'),
                                                                   (22, 'Kosmici'), (22, 'Cywile');

INSERT INTO aliases (character_id, aliases) VALUES
                                                (16, 'Domino Jachaś'), (16, 'Jahaś'),
                                                (17, 'Bogusław Łęcina'),
                                                (20, 'Mateusz');

-- BARDZO WAŻNE: Aktualizacja sekwencji tabeli character_card po sztywnych insertach
SELECT setval('character_card_id_seq', (SELECT MAX(id) FROM character_card));


-- 5. NEW QUOTES INSERT - DYNAMIC WITH CTE

-- Quote 1: Sebastian Bąk
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (2, 'A co by pan zrobil gdyby sie pan podiwedzial.. yy.. na przyklad... o. niech będzie takie pierwsze z brzegu. Co by pan zrobił gdyby się pan dowiedział', 'że pańska siorka opierdala kiełbachy kosmitom?', 'SPEAKER', 57)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'że pańska siorka opierdala pęto kosmitom?' FROM new_q UNION ALL
SELECT id, 'że pański szwagier ciągnie prąd na lewo kablem od prodiża?' FROM new_q UNION ALL
SELECT id, 'że z jajca wykluł się skurwiwij?' FROM new_q UNION ALL
SELECT id, 'że pańska siorka opierdala kiełbachy kosmitom?' FROM new_q;

-- Quote 2: Sułtan Kosmitów
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (6, 'Martini. Jo sui Szewczyk Dratewka bataliero della bazyliko. Gileto la łepetino ci na placo', 'maksimalito sekundo łepetino', 'SPEAKER', 73)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'non felichita mia kutachi martini' FROM new_q UNION ALL
SELECT id, 'di diabete non komediante' FROM new_q UNION ALL
SELECT id, 'putanamia, culo el catzo' FROM new_q UNION ALL
SELECT id, 'maksimalito sekundo łepetino' FROM new_q;

-- Quote 3: Marcin Hobuca
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (19, '- No co jest? nie wchodzi w dupe. - Hahaha głupcze. To nie jest dupa, tylko', 'jajca', 'SPEAKER', 73)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'morda' FROM new_q UNION ALL
SELECT id, 'głowa' FROM new_q UNION ALL
SELECT id, 'jajca' FROM new_q;

-- Quote 4: Michał Głuś (a)
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (5, 'Gdzie sikorka mówi ja pierdole?', 'w dupie', 'SPEAKER', 74)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'w kujwdubie' FROM new_q UNION ALL
SELECT id, 'w zupie' FROM new_q UNION ALL
SELECT id, 'na zlocie fanowskim Borubara, bramkarza RKS Chuwdu w Kurvix' FROM new_q UNION ALL
SELECT id, 'w dupie' FROM new_q;

-- Quote 5: Michał Głuś (b)
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (5, '- Pamiętasz drogę? - Tak - Przez kibel potem na dół, na góre, na dół, yyy, na górę i na dół. - Błąd! wtedy wyszedłbyś w kiblu. ', 'Jeszcze raz na górę i na dół. Wtedy tezż wyjdziesz w kiblu, ale będzie to kibel w lunaparku poza murami.', 'SPEAKER', 74)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'Na końcu jeszcze w lewo. Wtedy spotkamy się za murami sułtanatu.' FROM new_q UNION ALL
SELECT id, 'Jeszcze raz na dół a później na górę. Wyjdziesz prosto za bramą sułtanatu.' FROM new_q UNION ALL
SELECT id, 'Jeszcze raz na górę i na dół. Wtedy tezż wyjdziesz w kiblu, ale będzie to kibel w lunaparku poza murami.' FROM new_q;

-- Quote 6: Michał Głuś (c)
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (5, 'Nie ważne co mam sercu. Ważne', 'co mam w dupie', 'SPEAKER', 74)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'że nienawidzę sułtana kosmitów tak samo jak wy.' FROM new_q UNION ALL
SELECT id, 'co mam na zewnątrz.' FROM new_q UNION ALL
SELECT id, 'co mam w dupie' FROM new_q;

-- Quote 7: Pani Sraciatello
WITH new_q AS (
    INSERT INTO quote (character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
        VALUES (11, 'Eh, na co mi to było. Że też władza uderzyła mi do głowy i', 'zaczęłam pić oliwę zamiast soku.', 'SPEAKER', 74)
        RETURNING id
)
INSERT INTO quote_options (quote_id, option_text)
SELECT id, 'zaczęłam pić wódę zamiast soku.' FROM new_q UNION ALL
SELECT id, 'zaczęłam jeść smalec zamiast chleba.' FROM new_q UNION ALL
SELECT id, 'zaczęłam pić oliwę zamiast soku.' FROM new_q;