ALTER TABLE service
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS default_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE service
SET default_duration_minutes = 60
WHERE default_duration_minutes IS NULL;

ALTER TABLE service
    ALTER COLUMN default_duration_minutes SET DEFAULT 60,
    ALTER COLUMN default_duration_minutes SET NOT NULL;