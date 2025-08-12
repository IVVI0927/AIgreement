-- Add contract analysis tables
CREATE TABLE contract_analyses (
    id BIGSERIAL PRIMARY KEY,
    analysis_id VARCHAR(36) UNIQUE NOT NULL,
    contract_id BIGINT NOT NULL,
    analysis_type VARCHAR(30) NOT NULL,
    analysis_result TEXT,
    risk_score DECIMAL(5,2),
    risk_level VARCHAR(10),
    identified_risks TEXT,
    recommendations TEXT,
    compliance_status BOOLEAN,
    compliance_violations TEXT,
    processing_time_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_contract_analyses_contract FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE
);

-- Create indexes for contract_analyses table
CREATE INDEX idx_analysis_contract ON contract_analyses(contract_id);
CREATE INDEX idx_analysis_status ON contract_analyses(status);
CREATE INDEX idx_analysis_created_at ON contract_analyses(created_at);

-- Add constraint to ensure analysis_id is UUID format
ALTER TABLE contract_analyses ADD CONSTRAINT chk_analysis_id_uuid 
CHECK (analysis_id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$');