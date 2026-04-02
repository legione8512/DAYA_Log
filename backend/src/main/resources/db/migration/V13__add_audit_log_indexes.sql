CREATE INDEX IF NOT EXISTS idx_audit_log_studio_created_at
    ON audit_log(studio_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_studio_entity_created_at
    ON audit_log(studio_id, entity_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_studio_action_created_at
    ON audit_log(studio_id, action, created_at DESC);