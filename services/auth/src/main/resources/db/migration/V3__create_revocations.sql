-- Create token revocations table
CREATE TABLE token_revocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_revocations_user_id ON token_revocations(user_id);
