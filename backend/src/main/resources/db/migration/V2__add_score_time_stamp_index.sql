-- src/main/resources/db/migration/V2__add_score_time_stamp_index.sql

CREATE INDEX idx_score_time_stamp
    ON score(score_time_stamp);
