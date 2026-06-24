ALTER TABLE player DROP CONSTRAINT fk_player_on_today_score;

ALTER TABLE anonymous_session ADD completed_modes_today JSONB DEFAULT '[]'::jsonb;
ALTER TABLE anonymous_session ADD score_timestamps JSONB DEFAULT '{}'::jsonb;

ALTER TABLE player ADD completed_modes_today JSONB DEFAULT '[]'::jsonb;

ALTER TABLE guess_list ADD game_mode VARCHAR(255);
ALTER TABLE score ADD game_mode VARCHAR(255);
ALTER TABLE score ADD player_id BIGINT;


UPDATE anonymous_session
SET completed_modes_today = '["CLASSIC"]'::jsonb
WHERE has_guessed_today = true;

UPDATE anonymous_session
SET score_timestamps = jsonb_build_object('CLASSIC', score_timestamp)
WHERE score_timestamp IS NOT NULL;

UPDATE guess_list SET game_mode = 'CLASSIC';

UPDATE score s
SET player_id = p.id,
    game_mode = 'CLASSIC'
FROM player p
WHERE p.today_score_id = s.id;

UPDATE player
SET completed_modes_today = '["CLASSIC"]'::jsonb
WHERE has_guessed_today = true;


ALTER TABLE score ADD CONSTRAINT FK_SCORE_ON_PLAYER FOREIGN KEY (player_id) REFERENCES player (id);
ALTER TABLE verification_token ADD CONSTRAINT FK_VERIFICATIONTOKEN_ON_PLAYER FOREIGN KEY (player_id) REFERENCES player (id);
ALTER TABLE aliases ADD CONSTRAINT fk_aliases_on_character_card FOREIGN KEY (character_id) REFERENCES character_card (id);

ALTER TABLE anonymous_session DROP COLUMN has_guessed_today;
ALTER TABLE anonymous_session DROP COLUMN score_timestamp;

ALTER TABLE player DROP COLUMN has_guessed_today;
ALTER TABLE player DROP COLUMN today_score_id;

ALTER TABLE admin_audit_log ALTER COLUMN description TYPE VARCHAR(255) USING (description::VARCHAR(255));
ALTER TABLE admin_pending_change ALTER COLUMN payload TYPE VARCHAR(255) USING (payload::VARCHAR(255));