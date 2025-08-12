-- Initial schema for LegalAI contract service
-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create contracts table
CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    contract_type VARCHAR(50),
    file_name VARCHAR(255),
    file_size BIGINT,
    file_hash VARCHAR(128),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version INTEGER DEFAULT 1,
    owner_id BIGINT NOT NULL,
    parent_contract_id BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_contracts_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_contracts_parent FOREIGN KEY (parent_contract_id) REFERENCES contracts(id)
);

-- Create indexes for users table
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);

-- Create indexes for contracts table
CREATE INDEX idx_contract_owner ON contracts(owner_id);
CREATE INDEX idx_contract_status ON contracts(status);
CREATE INDEX idx_contract_type ON contracts(contract_type);
CREATE INDEX idx_contract_created_at ON contracts(created_at);

-- Insert default admin user (password: 'password' - should be changed in production)
INSERT INTO users (username, email, password, first_name, last_name) 
VALUES ('admin', 'admin@legalai.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin', 'User');

INSERT INTO user_roles (user_id, role) 
VALUES ((SELECT id FROM users WHERE username = 'admin'), 'ADMIN');