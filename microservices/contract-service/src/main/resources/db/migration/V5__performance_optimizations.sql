-- Performance optimizations for PostgreSQL

-- Enable query planner optimizations
SET enable_hashjoin = on;
SET enable_mergejoin = on;
SET enable_nestloop = on;

-- Full-text search indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_fulltext_content 
ON contracts USING gin(to_tsvector('english', content));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_fulltext_title 
ON contracts USING gin(to_tsvector('english', title));

-- Partial indexes for common queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_active_recent 
ON contracts(updated_at DESC) 
WHERE is_deleted = FALSE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_high_risk 
ON contracts(risk_score DESC) 
WHERE risk_score > 70 AND is_deleted = FALSE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_pending_analysis 
ON contracts(created_at DESC) 
WHERE analysis_status = 'PENDING' AND is_deleted = FALSE;

-- Covering indexes to avoid table lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_dashboard_data 
ON contracts(owner_id, status) 
INCLUDE (id, title, risk_score, updated_at) 
WHERE is_deleted = FALSE;

-- BRIN index for time-series data
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_contracts_created_at_brin 
ON contracts USING brin(created_at);

-- Connection pooling optimization
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET work_mem = '4MB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';

-- Query performance optimizations
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;

-- Prepared statement optimizations
ALTER SYSTEM SET plan_cache_mode = 'force_generic_plan';

-- Auto-vacuum optimizations
ALTER SYSTEM SET autovacuum_vacuum_scale_factor = 0.1;
ALTER SYSTEM SET autovacuum_analyze_scale_factor = 0.05;

-- Create partition for large contract table (if needed)
-- CREATE TABLE contracts_2024 PARTITION OF contracts 
-- FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

-- Analyze tables to update statistics
ANALYZE contracts;
ANALYZE contract_analyses;
ANALYZE audit_logs;