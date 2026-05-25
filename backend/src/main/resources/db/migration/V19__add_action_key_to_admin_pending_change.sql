ALTER TABLE admin_pending_change
    ADD COLUMN IF NOT EXISTS action_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_admin_pending_change_action_key
    ON admin_pending_change (action_key);

