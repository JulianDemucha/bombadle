INSERT INTO quote (id, character_card_id, quote_beginning, correct_answer, quote_target, appearance_episode)
VALUES
    (1, 15, 'Nie czarodziejska, tylko magiczna, i nie fujarka, tylko flet', 'Magiczna flet', 'SPEAKER', 133),
    (2, 7, 'Niektórych może zdziwić fakt, że jestem za pan brat ze skurwolem i kurvinoxem', 'Jednak niewola zbliża nawet największych wrogów', 'SPEAKER', 56),
    (3, 1, 'Pfu, kurwa, zapomniałem zamówić podwójnej cebuli.', 'Mam tylko pojedynczą', 'SPEAKER', 56);

INSERT INTO quote_options (quote_id, option_text) VALUES
                                                      (1, 'Magiczny flet'),
                                                      (1, 'I nie magiczna, tylko czarodziejska. Czarodziejska flet.'),
                                                      (1, 'Magiczna flet');

INSERT INTO quote_options (quote_id, option_text) VALUES
                                                      (2, 'Otóż nie jestem'),
                                                      (2, 'Jednak niewola zbliża nawet największych wrogów'),
                                                      (2, 'Otóż ja też jestem kosmitą, więc to całkiem normalne');

INSERT INTO quote_options (quote_id, option_text) VALUES
                                                      (3, 'Mam tylko pojedynczą'),
                                                      (3, 'Nie lubię cebuli'),
                                                      (3, 'Elf z cebulą dobry. Jak pomidor.');