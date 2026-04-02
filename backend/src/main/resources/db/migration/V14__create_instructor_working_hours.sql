CREATE TABLE instructor_working_hours (
    id UUID PRIMARY KEY,
    studio_id UUID NOT NULL REFERENCES studio(id),
    instructor_id UUID NOT NULL REFERENCES instructor(id),
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_instructor_working_hours_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_iwh_instructor_day
    ON instructor_working_hours(studio_id, instructor_id, day_of_week, active);

CREATE INDEX idx_iwh_instructor
    ON instructor_working_hours(instructor_id);