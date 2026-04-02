CREATE INDEX idx_client_studio ON client(studio_id);
CREATE INDEX idx_client_email_lower ON client(LOWER(email));
CREATE INDEX idx_client_first_name_lower ON client(LOWER(first_name));
CREATE INDEX idx_client_last_name_lower ON client(LOWER(last_name));
CREATE INDEX idx_client_phone ON client(phone);

CREATE INDEX idx_appointment_studio_start ON appointment(studio_id, start_at);
CREATE INDEX idx_appointment_studio_status_start ON appointment(studio_id, status, start_at);
CREATE INDEX idx_appointment_instructor_start ON appointment(instructor_id, start_at);
CREATE INDEX idx_appointment_participant_client ON appointment_participant(client_id);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_email_verification_user ON email_verification_token(user_id);
CREATE INDEX idx_password_reset_user ON password_reset_token(user_id);