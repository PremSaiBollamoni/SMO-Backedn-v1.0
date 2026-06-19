-- Flyway Migration: V2.0.0
-- Purpose: Add encryption support and audit logging for sensitive data
-- Date: 2026-06-19
-- Status: CRITICAL SECURITY UPDATE

-- ==========================================
-- 1. ADD PASSWORD MANAGEMENT COLUMNS
-- ==========================================

ALTER TABLE login ADD COLUMN password_changed_at DATETIME DEFAULT CURRENT_TIMESTAMP AFTER password;
ALTER TABLE login ADD COLUMN password_hash_version INT DEFAULT 2 AFTER password_changed_at;  -- 1=plain, 2=bcrypt
ALTER TABLE login ADD COLUMN failed_login_attempts INT DEFAULT 0 AFTER password_hash_version;
ALTER TABLE login ADD COLUMN last_login_attempt DATETIME DEFAULT NULL AFTER failed_login_attempts;
ALTER TABLE login ADD COLUMN locked_until DATETIME DEFAULT NULL AFTER last_login_attempt;
ALTER TABLE login ADD COLUMN locked BOOLEAN DEFAULT FALSE AFTER locked_until;

-- ==========================================
-- 2. ADD ENCRYPTION KEY VERSIONING
-- ==========================================

ALTER TABLE employee ADD COLUMN encryption_key_version INT DEFAULT 1 AFTER status;

-- ==========================================
-- 3. ADD ENCRYPTED FIELD COLUMNS
-- ==========================================
-- NOTE: These store encrypted values of sensitive fields
-- Original fields will be retained for backward compatibility during migration

ALTER TABLE employee
ADD COLUMN encrypted_aadhar_number VARCHAR(512) AFTER aadhar_number,
ADD COLUMN encrypted_pan_card_number VARCHAR(512) AFTER pan_card_number,
ADD COLUMN encrypted_phone VARCHAR(512) AFTER phone,
ADD COLUMN encrypted_salary VARCHAR(512) AFTER salary,
ADD COLUMN encrypted_dob VARCHAR(512) AFTER dob,
ADD COLUMN encrypted_emergency_contact VARCHAR(512) AFTER emergency_contact,
ADD COLUMN encrypted_blood_group VARCHAR(512) AFTER blood_group,
ADD COLUMN encrypted_address VARCHAR(512) AFTER address;

-- ==========================================
-- 4. CREATE AUDIT LOG TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,  -- LOGIN, PASSWORD_CHANGE, ROLE_CHANGE, DATA_ACCESS, CREATE, UPDATE, DELETE
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT,
    emp_id BIGINT NOT NULL,
    affected_emp_id BIGINT,  -- If operation is on another employee
    field_name VARCHAR(100),
    old_value LONGTEXT,
    new_value LONGTEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    status VARCHAR(20) DEFAULT 'SUCCESS',  -- SUCCESS, FAILED
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,

    -- Indexes for querying
    INDEX idx_emp_id (emp_id),
    INDEX idx_affected_emp_id (affected_emp_id),
    INDEX idx_created_at (created_at),
    INDEX idx_operation_type (operation_type),
    INDEX idx_table_name (table_name),
    CONSTRAINT fk_audit_emp_id FOREIGN KEY (emp_id) REFERENCES employee(emp_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- 5. CREATE LOGIN AUDIT TABLE (Separate for compliance)
-- ==========================================

CREATE TABLE IF NOT EXISTS login_audit (
    login_audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL,
    login_status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, LOCKED
    failure_reason VARCHAR(200),
    attempt_count INT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    location VARCHAR(100),
    device_type VARCHAR(50),
    attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Indexes
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (login_status),
    INDEX idx_attempted_at (attempted_at),
    CONSTRAINT fk_login_audit_emp_id FOREIGN KEY (emp_id) REFERENCES employee(emp_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- 6. CREATE PASSWORD HISTORY TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS password_history (
    password_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL,
    old_password_hash VARCHAR(255) NOT NULL,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    changed_by BIGINT,
    reason VARCHAR(100),  -- USER_CHANGE, ADMIN_RESET, PASSWORD_EXPIRED, SECURITY_ALERT

    -- Indexes
    INDEX idx_emp_id (emp_id),
    INDEX idx_changed_at (changed_at),
    CONSTRAINT fk_password_history_emp_id FOREIGN KEY (emp_id) REFERENCES employee(emp_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- 7. DATA MIGRATION - Hash existing plain text passwords
-- ==========================================
-- IMPORTANT: This requires application-side encryption using BCrypt
-- NOTE: This is a placeholder - actual password hashing must be done by application
-- The application will read plain passwords, hash them, and update the database

UPDATE login SET password_hash_version = 1 WHERE password_hash_version IS NULL;
-- Password hashing will be done by application in separate step

-- ==========================================
-- 8. CREATE SENSITIVE DATA ACCESS LOG
-- ==========================================

CREATE TABLE IF NOT EXISTS sensitive_data_access_log (
    access_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL,  -- User accessing data
    target_emp_id BIGINT NOT NULL,  -- Employee whose data is accessed
    field_accessed VARCHAR(100) NOT NULL,  -- aadhar_number, pan_card_number, salary, etc.
    action VARCHAR(50) NOT NULL,  -- READ, UPDATE, EXPORT
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    accessed_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Indexes
    INDEX idx_emp_id (emp_id),
    INDEX idx_target_emp_id (target_emp_id),
    INDEX idx_accessed_at (accessed_at),
    CONSTRAINT fk_sensitive_access_emp_id FOREIGN KEY (emp_id) REFERENCES employee(emp_id) ON DELETE SET NULL,
    CONSTRAINT fk_sensitive_access_target_emp_id FOREIGN KEY (target_emp_id) REFERENCES employee(emp_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- 9. GRANT MINIMAL PRIVILEGES
-- ==========================================
-- IMPORTANT: Update username/host as needed for your environment

-- Create application user with minimal permissions
-- Note: This should be executed separately with proper permissions
-- CREATE USER 'palms_app'@'localhost' IDENTIFIED BY 'strong_password_here';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON palmsv1.* TO 'palms_app'@'localhost';
-- REVOKE ALL PRIVILEGES ON palmsv1.* FROM 'palms_app'@'localhost';
-- REVOKE GRANT OPTION ON palmsv1.* FROM 'palms_app'@'localhost';

-- ==========================================
-- 10. CREATE ENCRYPTION KEY MANAGEMENT
-- ==========================================

CREATE TABLE IF NOT EXISTS encryption_key_metadata (
    key_version INT PRIMARY KEY,
    algorithm VARCHAR(50) NOT NULL,  -- AES-256
    key_id VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    retired_at DATETIME,
    is_active BOOLEAN DEFAULT TRUE,

    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial encryption key version
INSERT INTO encryption_key_metadata (key_version, algorithm, key_id, is_active)
VALUES (1, 'AES-256', 'default_key_v1', TRUE);

-- ==========================================
-- 11. DATABASE CONFIGURATION
-- ==========================================

-- Enable binary logging for audit trail
-- SET GLOBAL binlog_format = 'ROW';

-- ==========================================
-- VALIDATION QUERIES (Run after migration)
-- ==========================================
/*
-- Verify new columns exist
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='login' AND COLUMN_NAME IN ('password_changed_at', 'password_hash_version', 'failed_login_attempts');

-- Verify new tables exist
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA='PALMSV1' AND TABLE_NAME IN ('audit_log', 'login_audit', 'password_history', 'sensitive_data_access_log');

-- Check column sizes (should be 512 for encrypted fields)
SELECT COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='employee' AND COLUMN_NAME LIKE 'encrypted_%';
*/

-- ==========================================
-- COMPLETION
-- ==========================================
-- Status: Schema migration completed
-- Next: Run application password migration to hash existing plain-text passwords
-- Then: Update application code to use encryption for PII fields
-- Finally: Verify all audits are logged properly
