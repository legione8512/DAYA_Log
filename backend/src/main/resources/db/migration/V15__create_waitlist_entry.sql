CREATE TYPE waitlist_status AS ENUM ('ACTIVE', 'REMOVED', 'PROMOTED');

CREATE TABLE waitlist_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id) ON DELETE CASCADE,
    appointment_id UUID NOT NULL REFERENCES appointment(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    status waitlist_status NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_waitlist_entry_appointment_status_position
    ON waitlist_entry(appointment_id, status, position);

CREATE INDEX idx_waitlist_entry_client
    ON waitlist_entry(client_id);

CREATE UNIQUE INDEX uq_waitlist_entry_active_client
    ON waitlist_entry(appointment_id, client_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_waitlist_entry_active_position
    ON waitlist_entry(appointment_id, position)
    WHERE status = 'ACTIVE';