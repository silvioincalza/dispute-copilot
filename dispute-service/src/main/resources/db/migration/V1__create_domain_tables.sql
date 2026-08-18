CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    sca_status VARCHAR(32) NOT NULL,
    three_ds_version VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    reason_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    agent_notes TEXT,
    CONSTRAINT fk_disputes_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE TABLE IF NOT EXISTS chargeback_cases (
    id UUID PRIMARY KEY,
    dispute_id UUID NOT NULL UNIQUE,
    visa_mastercard_reason_code VARCHAR(100) NOT NULL,
    evidence_summary TEXT NOT NULL,
    filed_at TIMESTAMPTZ,
    pending_confirmation BOOLEAN NOT NULL,
    CONSTRAINT fk_chargeback_dispute FOREIGN KEY (dispute_id) REFERENCES disputes (id)
);
