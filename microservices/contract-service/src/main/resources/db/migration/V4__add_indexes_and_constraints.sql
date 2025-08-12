-- Add additional indexes and constraints for performance
-- Add composite indexes for common query patterns
CREATE INDEX idx_contracts_owner_status ON contracts(owner_id, status);
CREATE INDEX idx_contracts_type_status ON contracts(contract_type, status) WHERE is_deleted = FALSE;

-- Add unique constraint for active users (soft delete support)
CREATE UNIQUE INDEX idx_users_username_active ON users(username) WHERE is_active = TRUE;
CREATE UNIQUE INDEX idx_users_email_active ON users(email) WHERE is_active = TRUE;

-- Add check constraints for data integrity
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_version_positive 
CHECK (version > 0);

ALTER TABLE contracts ADD CONSTRAINT chk_contracts_file_size_positive 
CHECK (file_size IS NULL OR file_size > 0);

ALTER TABLE contract_analyses ADD CONSTRAINT chk_risk_score_range 
CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100));

-- Add function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add triggers to automatically update updated_at columns
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_contracts_updated_at
    BEFORE UPDATE ON contracts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add database statistics refresh function for better query planning
CREATE OR REPLACE FUNCTION refresh_database_stats()
RETURNS void AS $$
BEGIN
    ANALYZE users;
    ANALYZE contracts;
    ANALYZE contract_analyses;
    ANALYZE audit_logs;
END;
$$ LANGUAGE plpgsql;