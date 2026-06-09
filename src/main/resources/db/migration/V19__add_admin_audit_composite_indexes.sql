CREATE INDEX idx_admin_audit_actor_created_at
    ON admin_audit_log (actor, created_at);

CREATE INDEX idx_admin_audit_action_created_at
    ON admin_audit_log (action, created_at);
