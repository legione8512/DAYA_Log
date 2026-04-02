CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id),
    user_id UUID UNIQUE REFERENCES app_user(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    county VARCHAR(100),
    postcode VARCHAR(20),
    date_of_birth DATE,
    gender gender_type,
    lead_source VARCHAR(100),
    gdpr_consent BOOLEAN NOT NULL DEFAULT FALSE,
    gdpr_consent_at TIMESTAMPTZ,
    email_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    sms_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_client_studio_email UNIQUE (studio_id, email),
    CONSTRAINT uk_client_studio_phone UNIQUE (studio_id, phone)
);

CREATE TABLE client_sensitive_profile (
    client_id UUID PRIMARY KEY REFERENCES client(id) ON DELETE CASCADE,
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(30),
    medical_notes TEXT,
    restrictions TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id),
    name VARCHAR(150) NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    price NUMERIC(10,2) NOT NULL DEFAULT 0,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_service_studio_name UNIQUE (studio_id, name),
    CONSTRAINT chk_service_duration_positive CHECK (duration_minutes > 0)
);

CREATE TABLE instructor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    specialisation VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_instructor_studio_email UNIQUE (studio_id, email)
);

CREATE TABLE resource (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL REFERENCES studio(id),
    name VARCHAR(150) NOT NULL,
    type resource_type NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_resource_studio_name UNIQUE (studio_id, name)
);