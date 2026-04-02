CREATE TABLE IF NOT EXISTS service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    default_duration_minutes INTEGER NOT NULL DEFAULT 60,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_service_studio ON service(studio_id);
CREATE INDEX IF NOT EXISTS idx_service_studio_name_lower ON service(studio_id, LOWER(name));