ALTER TABLE guess_list DROP CONSTRAINT uc_guesslist_player;
ALTER TABLE guess_list ADD CONSTRAINT uc_guesslist_player_mode UNIQUE (player_id, game_mode);