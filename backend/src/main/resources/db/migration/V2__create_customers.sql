CREATE TABLE customers (
                           id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           organization_id    UUID NOT NULL REFERENCES organizations (id),
                           external_id        VARCHAR(100) NOT NULL,
                           tenure_months      INTEGER NOT NULL CHECK (tenure_months >= 0),
                           phone_service      BOOLEAN NOT NULL,
                           multiple_lines     VARCHAR(30) NOT NULL,
                           internet_service   VARCHAR(30) NOT NULL,
                           online_security    VARCHAR(30) NOT NULL,
                           online_backup      VARCHAR(30) NOT NULL,
                           device_protection  VARCHAR(30) NOT NULL,
                           tech_support       VARCHAR(30) NOT NULL,
                           streaming_tv       VARCHAR(30) NOT NULL,
                           streaming_movies   VARCHAR(30) NOT NULL,
                           contract           VARCHAR(30) NOT NULL,
                           paperless_billing  BOOLEAN NOT NULL,
                           payment_method     VARCHAR(50) NOT NULL,
                           monthly_charges    NUMERIC(10, 2) NOT NULL CHECK (monthly_charges >= 0),
                           total_charges      NUMERIC(12, 2) CHECK (total_charges >= 0),
                           churned            BOOLEAN,
                           created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                           CONSTRAINT uq_customers_org_external UNIQUE (organization_id, external_id)
);

CREATE INDEX idx_customers_organization ON customers (organization_id);