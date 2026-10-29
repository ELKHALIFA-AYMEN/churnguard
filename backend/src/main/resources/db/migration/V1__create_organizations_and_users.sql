CREATE TABLE organizations (
                               id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               name        VARCHAR(150) NOT NULL,
                               created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE users (
                       id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       organization_id  UUID NOT NULL REFERENCES organizations (id),
                       email            VARCHAR(255) NOT NULL UNIQUE,
                       password_hash    VARCHAR(255) NOT NULL,
                       role             VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'ANALYST', 'VIEWER')),
                       created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);