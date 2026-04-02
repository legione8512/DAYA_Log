CREATE INDEX IF NOT EXISTS idx_refresh_token_hash ON refresh_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_hash ON email_verification_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires_at ON password_reset_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_expires_at ON email_verification_token(expires_at);