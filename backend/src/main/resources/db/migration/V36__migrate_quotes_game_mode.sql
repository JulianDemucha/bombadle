UPDATE current_card_mapping SET game_mode = 'QUOTES_STAGE_2' WHERE game_mode = 'QUOTES';
UPDATE previous_card_mapping SET game_mode = 'QUOTES_STAGE_2' WHERE game_mode = 'QUOTES';

UPDATE score SET game_mode = 'QUOTES_STAGE_2' WHERE game_mode = 'QUOTES';
UPDATE guess_list SET game_mode = 'QUOTES_STAGE_2' WHERE game_mode = 'QUOTES';
UPDATE anonymous_guess_list SET game_mode = 'QUOTES_STAGE_2' WHERE game_mode = 'QUOTES';