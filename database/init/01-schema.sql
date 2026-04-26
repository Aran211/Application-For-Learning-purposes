CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS memos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_memos_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_memos_user_id ON memos (user_id);
CREATE INDEX IF NOT EXISTS idx_memos_updated_at ON memos (updated_at DESC);

CREATE TABLE IF NOT EXISTS workout_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    exercise VARCHAR(100) NOT NULL,
    sets_completed INTEGER NOT NULL CHECK (sets_completed >= 1),
    reps_completed INTEGER NOT NULL CHECK (reps_completed >= 1),
    weight_kg DOUBLE PRECISION NOT NULL CHECK (weight_kg >= 0),
    workout_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_workout_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workout_logs_user_id ON workout_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_workout_logs_workout_date ON workout_logs (workout_date DESC);

CREATE TABLE IF NOT EXISTS issued_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_issued_tokens_token_id ON issued_tokens (token_id);
CREATE INDEX IF NOT EXISTS idx_issued_tokens_user_id ON issued_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_issued_tokens_expires_at ON issued_tokens (expires_at);
