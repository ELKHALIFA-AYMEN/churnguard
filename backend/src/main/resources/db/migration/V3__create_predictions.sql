CREATE TABLE predictions (
                             id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             organization_id  UUID NOT NULL REFERENCES organizations (id),
                             customer_id      UUID NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
                             probability      NUMERIC(5, 4) NOT NULL CHECK (probability >= 0 AND probability <= 1),
                             at_risk          BOOLEAN NOT NULL,
                             threshold        NUMERIC(5, 4) NOT NULL,
                             factors          TEXT NOT NULL,
                             predicted_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                             CONSTRAINT uq_predictions_customer UNIQUE (customer_id)
);

CREATE INDEX idx_predictions_org_probability
    ON predictions (organization_id, probability DESC);