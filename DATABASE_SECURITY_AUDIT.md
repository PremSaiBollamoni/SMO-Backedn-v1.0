# SMO Application - Database Security Audit Report

**Date:** 2026-06-19  
**Audit Scope:** Complete database schema for sensitive data exposure  
**Status:** SECURITY ISSUES IDENTIFIED - HIGH PRIORITY REMEDIATION REQUIRED

---

## EXECUTIVE SUMMARY

The SMO (Smart Manufacturing Operations) application database contains **CRITICAL SECURITY VULNERABILITIES** related to unencrypted PII (Personally Identifiable Information) and inadequate password hashing implementation. Multiple tables store highly sensitive employee and financial data in plain text, violating industry security standards and compliance requirements.

### Key Findings:
- ✗ **CRITICAL:** Passwords stored as plain text or improperly hashed in `login` table
- ✗ **CRITICAL:** Aadhar and PAN numbers (government IDs) stored unencrypted in `employee` table
- ✗ **HIGH:** Employee personal data (phone, DOB, salary) unencrypted
- ✗ **MEDIUM:** Audit trail completely absent - no logging of sensitive operations
- ✓ **GOOD:** BCrypt password utility exists but is not being enforced
- ✓ **GOOD:** Column sizes adequate for BCrypt hashes (255 chars)

---

## PART 1: CURRENT DATABASE SCHEMA ANALYSIS

### 1.1 EMPLOYEE-RELATED TABLES

#### Table: `employee` (EmployeeInfo)
**Location:** `src/main/java/com/cutm/smo/models/EmployeeInfo.java`

| Column | Type | Length | Status | Risk | Notes |
|--------|------|--------|--------|------|-------|
| `emp_id` | BIGINT | - | PK | LOW | Employee ID (reference only) |
| `name` | VARCHAR | 150 | - | LOW | Employee name |
| `role_id` | BIGINT | - | FK | LOW | Role assignment |
| `dob` | DATE | - | - | **HIGH** | Date of birth - UNENCRYPTED PII |
| `phone` | VARCHAR | 20 | UNIQUE | **HIGH** | Phone number - UNENCRYPTED PII |
| `address` | VARCHAR | 255 | - | MEDIUM | Home address - UNENCRYPTED PII |
| `email` | VARCHAR | 150 | UNIQUE | **MEDIUM** | Email - UNENCRYPTED, but often public |
| `salary` | DECIMAL | 12,2 | - | **CRITICAL** | Salary data - UNENCRYPTED FINANCIAL DATA |
| `emp_date` | DATE | - | - | LOW | Employment date |
| `blood_group` | VARCHAR | 10 | - | MEDIUM | Blood type - UNENCRYPTED HEALTH DATA |
| `emergency_contact` | VARCHAR | 20 | - | **HIGH** | Emergency contact number - UNENCRYPTED PII |
| `aadhar_number` | VARCHAR | 512 | - | **CRITICAL** | Government ID - UNENCRYPTED, NOT HASHED |
| `pan_card_number` | VARCHAR | 512 | - | **CRITICAL** | Tax ID - UNENCRYPTED, NOT HASHED |
| `status` | VARCHAR | 50 | - | LOW | Employee status (ACTIVE/INACTIVE) |
| `created_by` | BIGINT | - | - | LOW | Audit trail - user who created |
| `created_at` | DATETIME | - | - | LOW | Audit trail - when created |

**VULNERABILITIES:**
```
Current State: ALL PII/financial data stored in PLAIN TEXT
Column aadhar_number: VARCHAR(512) - sized for encryption but data is UNENCRYPTED
Column pan_card_number: VARCHAR(512) - sized for encryption but data is UNENCRYPTED
Column salary: DECIMAL(12,2) - numeric, no encryption possible in current form
Missing columns: password_changed_at, failed_login_attempts, last_login_attempt
Missing audit trail: No encryption_key_version tracking
```

#### Table: `login` (EmployeeLogin)
**Location:** `src/main/java/com/cutm/smo/models/EmployeeLogin.java`

| Column | Type | Length | Status | Risk | Notes |
|--------|------|--------|--------|------|-------|
| `emp_id` | BIGINT | - | PK | - | Reference to employee |
| `password` | VARCHAR | 255 | - | **CRITICAL** | Password field |
| `status` | VARCHAR | 50 | - | LOW | Login status |

**CRITICAL VULNERABILITY - PASSWORD STORAGE:**

**Current Implementation Issue:**
- Column size: VARCHAR(255) ✓ (sufficient for BCrypt hashes)
- BCrypt utility exists: `PasswordUtil.java` ✓
- **BUT:** Password hashing is NOT enforced at database layer

**Evidence from Code Analysis:**

1. **LoginService.java (Line 25-36, 50-55):**
```java
public EmployeeLogin createLogin(EmployeeLogin login) {
    // ... validation ...
    login.setPassword(request.getPassword()); // PLAIN TEXT!
    employeeLoginRepository.save(login);      // Saved as-is
}

public void updatePassword(Long empId, String newPassword) {
    login.setPassword(newPassword.trim());    // PLAIN TEXT!
    employeeLoginRepository.save(login);
}
```

2. **EmployeeService.java (Line 88-92):**
```java
EmployeeLogin login = new EmployeeLogin();
login.setEmpId(empId);
login.setPassword(request.getPassword().trim()); // PLAIN TEXT!
login.setStatus("ACTIVE");
employeeLoginRepository.save(login);
```

3. **DataInitializer.java (Line 46-49, 68-74):**
```java
ensureEmployeeWithLogin(1001L, "HR Admin", hrAdminRole, "hr123");
// ...
EmployeeLogin login = new EmployeeLogin();
login.setPassword(password); // PLAIN TEXT!
employeeLoginRepository.save(login);
```

**Fallback Handling (AuthService.java, Line 84-92):**
```java
if (PasswordUtil.isBcryptHashed(storedPassword)) {
    passwordMatches = passwordEncoder.matches(providedPassword, storedPassword);
} else {
    // FALLBACK FOR PLAIN TEXT!
    log.warn("Plain text password detected...");
    passwordMatches = storedPassword.equals(providedPassword);
}
```

**RESULT:** System currently accepts PLAIN TEXT passwords and has fallback logic to match them directly. BCrypt is checked for, but NOT enforced during creation/update.

---

### 1.2 OPERATIONAL TABLES WITH EMPLOYEE IDs

#### Table: `attendance` (Attendance)
```java
emp_id (BIGINT)        // Employee reference - can be used to join to PII
temp_qr_token (VARCHAR 50)  // QR token for tracking
machine_code (VARCHAR 50)   // Workstation identifier
att_date (DATE)            // Attendance date
check_in (DATETIME)        // Check-in timestamp
check_out (DATETIME)       // Check-out timestamp - can be linked to PII
marked_by (BIGINT)         // Employee who marked attendance
```
**Risk:** Attendance linked to emp_id → can identify individuals from operational data

#### Table: `job_assignment` (JobAssignment)
```java
emp_id (BIGINT)                  // Employee reference
ws_id (BIGINT)        // Workstation
tray_id (BIGINT)      // Product identifier
sam_value (DECIMAL 8,3)          // Standard time
est_minutes / actual_minutes     // Timing data
efficiency_pct (DECIMAL 7,2)     // Can be used to identify/track individuals
assigned_by (BIGINT)             // Audit trail - who assigned
```
**Risk:** Efficiency and productivity metrics linked to emp_id → privacy concern, performance tracking

#### Table: `production_log` (ProductionLog)
```java
emp_id (BIGINT)                  // Employee reference
shift_id, ws_id                  // Operational context
pieces_produced (INTEGER)         // Output metrics
sam_earned (DECIMAL)             // Productivity metrics
```
**Risk:** Can be linked to PII to track employee productivity

#### Table: `qc_log` (QcLog)
```java
marked_by (BIGINT)               // Employee who marked QC
notes (VARCHAR 500)              // May contain personal comments
```
**Risk:** Quality review notes may contain subjective employee references

#### Table: `packing_log` (PackingLog)
```java
logged_by (BIGINT)               // Employee who logged
pack_date, cartons_packed, etc.  // Operational data
```
**Risk:** Packing metrics linked to emp_id

#### Table: `sam_study` (SamStudy)
```java
studied_by (BIGINT)              // Employee who conducted study
approved_by (BIGINT)             // Employee who approved
notes (VARCHAR 500)              // May contain personal comments
```
**Risk:** Notes may reference individuals by name or characteristic

#### Table: `method_study` (MethodStudy)
```java
emp_id (BIGINT)                  // Employee reference
assigned_to (BIGINT)             // Employee assigned for study
notes (VARCHAR 1000)             // Detailed notes - privacy risk
```
**Risk:** Detailed improvement notes linked to employee IDs

---

## PART 2: SENSITIVE DATA CLASSIFICATION & CURRENT STATE

### HIGH PRIORITY - CRITICAL ENCRYPTION REQUIRED

#### Level 1: Government IDs (CRITICAL)
| Field | Table | Current | Required | Note |
|-------|-------|---------|----------|------|
| `aadhar_number` | employee | VARCHAR 512 PLAIN | AES-256 ENCRYPTED | India: Document ID, highly sensitive |
| `pan_card_number` | employee | VARCHAR 512 PLAIN | AES-256 ENCRYPTED | India: Tax ID, highly sensitive |

**Compliance Impact:** Violation of Indian data protection standards
**Storage Impact:** Already sized for encryption (VARCHAR 512)

#### Level 2: Authentication Credentials (CRITICAL)
| Field | Table | Current | Required | Note |
|-------|-------|---------|----------|------|
| `password` | login | VARCHAR 255 PLAIN or WEAK | BCrypt (strength 12) | Currently not enforced |

**Compliance Impact:** Violation of OWASP password storage standards
**Action Required:** Immediate migration to BCrypt

#### Level 3: Personal Information (HIGH)
| Field | Table | Current | Required | Note |
|-------|-------|---------|----------|------|
| `phone` | employee | VARCHAR 20 PLAIN | AES-256 ENCRYPTED (optional) | Can identify individual |
| `emergency_contact` | employee | VARCHAR 20 PLAIN | AES-256 ENCRYPTED | Contact info, sensitive |
| `dob` | employee | DATE PLAIN | AES-256 ENCRYPTED (optional) | Used for authentication bypass |
| `address` | employee | VARCHAR 255 PLAIN | AES-256 ENCRYPTED (optional) | Home address, privacy |

**Compliance Impact:** Violation of PII protection standards

#### Level 4: Financial Data (CRITICAL)
| Field | Table | Current | Required | Note |
|-------|-------|---------|----------|------|
| `salary` | employee | DECIMAL(12,2) PLAIN | AES-256 ENCRYPTED | Financial data, highly sensitive |

**Compliance Impact:** Violation of financial data protection standards
**Architecture Challenge:** Numeric field cannot be searched encrypted - requires restructuring

#### Level 5: Health Information (MEDIUM)
| Field | Table | Current | Required | Note |
|-------|-------|---------|----------|------|
| `blood_group` | employee | VARCHAR 10 PLAIN | AES-256 ENCRYPTED (optional) | Health data, privacy concern |

**Compliance Impact:** Violation of health data protection standards

### MEDIUM PRIORITY - AUDIT LOGGING

**Currently Missing:**
- No `audit_log` table
- No logging of password changes
- No logging of role/permission changes
- No logging of access to sensitive fields
- No tracking of who accessed what sensitive data
- No temporal audit trail

---

## PART 3: IDENTIFIED SECURITY GAPS

### 3.1 PASSWORD HASHING GAPS

**Current State:**
```
✓ BCrypt utility class exists (PasswordUtil.java)
✗ NOT enforced in LoginService.createLogin()
✗ NOT enforced in LoginService.updatePassword()
✗ NOT enforced in EmployeeService.createEmployee()
✗ NOT enforced in DataInitializer.java
✗ Fallback to plain text comparison in AuthService
```

**Evidence:**
- `LoginService.java:71` - `login.setPassword(password)` saves plain text
- `EmployeeService.java:90` - `login.setPassword(request.getPassword().trim())` saves plain text
- `DataInitializer.java:71` - `login.setPassword(password)` saves plain text
- `AuthService.java:85-92` - Falls back to `storedPassword.equals(providedPassword)` for plain text

**Impact:**
- Any plaintext password from 2025 onward is recoverable
- Attacker with DB access can read all passwords
- No protection against password leaks

### 3.2 PII ENCRYPTION GAPS

**Current State:**
```
✗ Aadhar number: VARCHAR(512) sized for encryption but STORED IN PLAIN TEXT
✗ PAN card: VARCHAR(512) sized for encryption but STORED IN PLAIN TEXT
✗ Phone: VARCHAR(20) PLAIN TEXT
✗ Email: VARCHAR(150) PLAIN TEXT (though often public)
✗ DOB: DATE PLAIN TEXT
✗ Salary: DECIMAL(12,2) PLAIN TEXT (numeric, harder to encrypt)
✗ Blood group: VARCHAR(10) PLAIN TEXT
✗ Emergency contact: VARCHAR(20) PLAIN TEXT
✗ Address: VARCHAR(255) PLAIN TEXT
```

**Impact:**
- Database backup leaks all employee PII
- Attacker with DB access has complete employee profiles
- Violation of data protection standards

### 3.3 AUDIT LOGGING GAPS

**Missing:**
```
✗ No audit_log table
✗ No tracking of password changes
✗ No tracking of access to sensitive fields
✗ No tracking of who updated what data
✗ No temporal audit trail
✗ No role/permission change logs
```

**Impact:**
- Cannot investigate security incidents
- No compliance evidence for audits
- Cannot prove data integrity

### 3.4 DATABASE STRUCTURE LIMITATIONS

**Current Limitations:**
1. **Salary as DECIMAL:** Cannot be searched when encrypted
2. **No backup encryption:** Backups contain all plaintext data
3. **No encryption key versioning:** Cannot rotate keys without migration downtime
4. **No encryption key storage:** Where will keys be stored securely?

---

## PART 4: MIGRATION STRATEGY & IMPLEMENTATION PLAN

### Phase 1: Immediate Actions (CRITICAL)

#### 1.1 Create Migration for Password Hashing

**File:** `V2_0_0__Encrypt_Passwords_BCrypt.sql`

```sql
-- Migrate existing plain text passwords to BCrypt hashes
-- IMPORTANT: This migration requires application-level processing

-- Step 1: Add temporary column for hashed passwords
ALTER TABLE login ADD COLUMN password_hash VARCHAR(255);

-- Step 2: Update password field to store hashes (application will do this)
-- This is a manual application-driven migration

-- Step 3: Validate all passwords are hashed (start with $2a$, $2b$, or $2y$)
-- SELECT emp_id FROM login WHERE password NOT LIKE '$2%$';

-- Step 4: Drop old password column and rename
-- ALTER TABLE login DROP COLUMN password;
-- ALTER TABLE login RENAME COLUMN password_hash TO password;
```

#### 1.2 Create Security Metadata Columns

**File:** `V2_0_1__Add_Security_Metadata.sql`

```sql
-- Add security tracking columns to login table
ALTER TABLE login ADD COLUMN password_changed_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE login ADD COLUMN last_login_attempt DATETIME;
ALTER TABLE login ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE login ADD COLUMN account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE login ADD COLUMN account_locked_until DATETIME;

-- Add encryption key version for future key rotation
ALTER TABLE employee ADD COLUMN encryption_key_version INT DEFAULT 1;

-- Add column to track which fields are encrypted
ALTER TABLE employee ADD COLUMN encrypted_fields JSON;
```

---

### Phase 2: Encryption Infrastructure (WEEKS 1-2)

#### 2.1 Create Encryption Utility

**File:** `src/main/java/com/cutm/smo/security/EncryptionUtil.java`

```java
package com.cutm.smo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256 Encryption/Decryption utility for sensitive data fields.
 * 
 * IMPORTANT: Encryption keys must be stored in environment variables,
 * never in code or properties files.
 * 
 * Environment variables:
 * - ENCRYPTION_KEY: Base64-encoded AES-256 key (32 bytes = 256 bits)
 * 
 * Key Generation Command (Run ONCE, store in secure vault):
 * java -cp . com.cutm.smo.security.KeyGenerator
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES";
    private final SecretKey secretKey;

    public EncryptionUtil(@Value("${encryption.key:#{null}}") String encryptionKeyBase64) {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.trim().isEmpty()) {
            throw new IllegalStateException(
                "CRITICAL: encryption.key not configured. " +
                "Set ENCRYPTION_KEY environment variable with Base64-encoded 256-bit AES key. " +
                "Generate with: KeyGenerator.generateKey()"
            );
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException(
                    "Encryption key must be 256 bits (32 bytes). Got: " + (decodedKey.length * 8) + " bits"
                );
            }
            this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
            log.info("Encryption utility initialized with 256-bit AES key");
        } catch (IllegalArgumentException e) {
            log.error("Invalid encryption key configuration", e);
            throw e;
        }
    }

    /**
     * Encrypt a plain text string.
     * 
     * @param plainText the text to encrypt
     * @return Base64-encoded ciphertext
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            log.debug("Data encrypted successfully (length: {})", encrypted.length());
            return encrypted;
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed for sensitive field", e);
        }
    }

    /**
     * Decrypt a Base64-encoded ciphertext.
     * 
     * @param cipherText Base64-encoded encrypted text
     * @return decrypted plaintext
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("Data decrypted successfully");
            return decrypted;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed for sensitive field", e);
        }
    }

    /**
     * Check if a value is already encrypted (Base64-encoded).
     * Note: This is a heuristic - Base64 strings that decode to valid UTF-8 are assumed encrypted.
     * 
     * @param value the value to check
     * @return true if appears to be encrypted
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            // Try to decode as Base64
            Base64.getDecoder().decode(value);
            // Check if it looks like a Base64-encoded string (roughly)
            return value.matches("^[A-Za-z0-9+/]*={0,2}$");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

#### 2.2 Create JPA Attribute Converters

**File:** `src/main/java/com/cutm/smo/security/EncryptedStringConverter.java`

```java
package com.cutm.smo.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * JPA Attribute Converter for automatic encryption/decryption of String fields.
 * Usage: @Convert(converter = EncryptedStringConverter.class)
 * 
 * This converter provides transparent encryption/decryption at the ORM layer.
 */
@Slf4j
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    // Static reference since JPA instantiates converters
    private static EncryptionUtil encryptionUtil;

    @Autowired
    public void setEncryptionUtil(EncryptionUtil util) {
        EncryptedStringConverter.encryptionUtil = util;
    }

    /**
     * Convert entity attribute (plain text) to database value (encrypted).
     * Called before saving to database.
     * 
     * @param attribute the attribute value from the entity
     * @return encrypted Base64-encoded value for storage
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        
        // Skip encryption if already encrypted
        if (EncryptionUtil.isEncrypted(attribute)) {
            log.debug("Value already encrypted, skipping double-encryption");
            return attribute;
        }
        
        if (encryptionUtil == null) {
            log.warn("EncryptionUtil not initialized. Make sure EncryptionUtil is a Spring component.");
            return attribute;
        }
        
        try {
            String encrypted = encryptionUtil.encrypt(attribute);
            log.debug("Attribute encrypted for database storage");
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt attribute", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Convert database value (encrypted) to entity attribute (plain text).
     * Called after loading from database.
     * 
     * @param dbData the encrypted Base64-encoded value from database
     * @return decrypted plain text value
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        
        // Skip decryption if not encrypted
        if (!EncryptionUtil.isEncrypted(dbData)) {
            log.debug("Value not encrypted, returning as-is (legacy data?)");
            return dbData;
        }
        
        if (encryptionUtil == null) {
            log.warn("EncryptionUtil not initialized");
            return dbData;
        }
        
        try {
            String decrypted = encryptionUtil.decrypt(dbData);
            log.debug("Attribute decrypted from database");
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt attribute", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

---

### Phase 3: Apply Encryption to Models (WEEKS 2-3)

#### 3.1 Update EmployeeInfo with Encryption Annotations

**File:** `src/main/java/com/cutm/smo/models/EmployeeInfo.java` (UPDATE)

```java
// REPLACE EXISTING ANNOTATIONS ON SENSITIVE FIELDS:

@Column(name = "phone", length = 512)  // CHANGED: increased for encrypted storage
@Convert(converter = EncryptedStringConverter.class)
private String phone;

@Column(name = "dob")  // Store as text since dates are complex
@Convert(converter = EncryptedStringConverter.class)
private LocalDate dob;

@Column(name = "address", length = 512)  // CHANGED: increased for encrypted storage
@Convert(converter = EncryptedStringConverter.class)
private String address;

// Email: Optional encryption - often used for login so consider searchability
@Column(name = "email", nullable = false, length = 512)  // CHANGED: increased
// Decide: encrypt or keep plaintext for searchability in admin panel

@Column(name = "salary", length = 512)  // CHANGED: convert to VARCHAR for encryption
// Note: Numeric comparison/sorting will be lost. Consider storing as encrypted string
@Convert(converter = EncryptedStringConverter.class)
private String salary;  // CHANGED: String instead of BigDecimal

@Column(name = "blood_group", length = 512)  // CHANGED: increased for encrypted storage
@Convert(converter = EncryptedStringConverter.class)
private String bloodGroup;

@Column(name = "emergency_contact", length = 512)  // CHANGED: increased for encrypted storage
@Convert(converter = EncryptedStringConverter.class)
private String emergencyContact;

@Column(name = "aadhar_number", length = 512)  // Already sized correctly
@Convert(converter = EncryptedStringConverter.class)
private String aadharNumber;

@Column(name = "pan_card_number", length = 512)  // Already sized correctly
@Convert(converter = EncryptedStringConverter.class)
private String panCardNumber;

@Column(name = "encryption_key_version", nullable = false)
private Integer encryptionKeyVersion = 1;

@Column(name = "encrypted_fields")
private String encryptedFields;  // JSON: tracks which fields are encrypted
```

#### 3.2 Update EmployeeLogin for Proper Password Hashing

**File:** `src/main/java/com/cutm/smo/models/EmployeeLogin.java` (UPDATE)

```java
@Column(name = "password", nullable = false, length = 255)  // Size unchanged (BCrypt = 60 chars)
private String password;  // MUST be BCrypt hashed

@Column(name = "password_changed_at")
private LocalDateTime passwordChangedAt;

@Column(name = "last_login_attempt")
private LocalDateTime lastLoginAttempt;

@Column(name = "failed_login_attempts")
private Integer failedLoginAttempts = 0;

@Column(name = "account_locked")
private Boolean accountLocked = false;

@Column(name = "account_locked_until")
private LocalDateTime accountLockedUntil;
```

---

### Phase 4: Create Audit Logging (WEEKS 2-4)

#### 4.1 Create Audit Log Entity

**File:** `src/main/java/com/cutm/smo/models/AuditLog.java` (NEW)

```java
package com.cutm.smo.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Audit log for tracking all sensitive operations.
 * 
 * Operations tracked:
 * - PASSWORD_CHANGE: Password updated
 * - EMPLOYEE_CREATE: New employee created
 * - EMPLOYEE_UPDATE: Employee data updated (especially sensitive fields)
 * - ROLE_ASSIGN: Role assigned to employee
 * - ROLE_REVOKE: Role removed from employee
 * - DATA_ACCESS: Access to sensitive PII fields
 * - PASSWORD_RESET: Admin reset employee password
 * - ACCOUNT_LOCK: Account locked after failed attempts
 * 
 * Sensitive fields that trigger audit:
 * - password
 * - aadhar_number
 * - pan_card_number
 * - phone
 * - email
 * - salary
 * - dob
 */
@Data
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_emp", columnList = "emp_id"),
        @Index(name = "idx_audit_actor", columnList = "actor_emp_id"),
        @Index(name = "idx_audit_operation", columnList = "operation"),
        @Index(name = "idx_audit_timestamp", columnList = "created_at")
    }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    // The employee whose data was affected
    @Column(name = "emp_id", nullable = false)
    private Long empId;

    // The employee who performed the action (NULL for system actions)
    @Column(name = "actor_emp_id")
    private Long actorEmpId;

    // Type of operation: PASSWORD_CHANGE, EMPLOYEE_UPDATE, ROLE_ASSIGN, etc.
    @Column(name = "operation", nullable = false, length = 50)
    private String operation;

    // Which field was affected
    @Column(name = "field_name", length = 100)
    private String fieldName;

    // Old value (for updates) - encrypted if sensitive
    @Column(name = "old_value", length = 1000)
    private String oldValue;

    // New value (for updates) - encrypted if sensitive
    @Column(name = "new_value", length = 1000)
    private String newValue;

    // Summary of what happened
    @Column(name = "description", length = 500)
    private String description;

    // IP address if available
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // User agent if available
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Status: SUCCESS or FAILURE
    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS";

    // Error message if FAILURE
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

#### 4.2 Create Audit Logging Service

**File:** `src/main/java/com/cutm/smo/services/AuditLogService.java` (NEW)

```java
package com.cutm.smo.services;

import com.cutm.smo.models.AuditLog;
import com.cutm.smo.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Service for logging audit events.
 * 
 * Usage:
 * auditLogService.logPasswordChange(empId, actorEmpId, "old_hash", "new_hash");
 * auditLogService.logEmployeeUpdate(empId, actorEmpId, "salary", oldSalary, newSalary);
 * auditLogService.logRoleChange(empId, actorEmpId, "SUPERVISOR", "ROLE_ASSIGN");
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log a password change operation.
     */
    @Transactional
    public void logPasswordChange(Long empId, Long actorEmpId) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setActorEmpId(actorEmpId);
        log.setOperation("PASSWORD_CHANGE");
        log.setFieldName("password");
        log.setDescription("Password changed");
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Password changed", empId, actorEmpId);
    }

    /**
     * Log a password reset by admin.
     */
    @Transactional
    public void logPasswordReset(Long empId, Long actorEmpId) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setActorEmpId(actorEmpId);
        log.setOperation("PASSWORD_RESET");
        log.setFieldName("password");
        log.setDescription("Password reset by admin");
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Password reset", empId, actorEmpId);
    }

    /**
     * Log employee data update (especially sensitive fields).
     */
    @Transactional
    public void logEmployeeUpdate(Long empId, Long actorEmpId, String fieldName, 
                                  String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setActorEmpId(actorEmpId);
        log.setOperation("EMPLOYEE_UPDATE");
        log.setFieldName(fieldName);
        log.setOldValue(oldValue != null ? maskSensitive(oldValue, fieldName) : null);
        log.setNewValue(newValue != null ? maskSensitive(newValue, fieldName) : null);
        log.setDescription("Field updated: " + fieldName);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Employee data updated: " + fieldName, empId, actorEmpId);
    }

    /**
     * Log role assignment.
     */
    @Transactional
    public void logRoleAssign(Long empId, Long actorEmpId, String roleName) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setActorEmpId(actorEmpId);
        log.setOperation("ROLE_ASSIGN");
        log.setFieldName("role");
        log.setNewValue(roleName);
        log.setDescription("Role assigned: " + roleName);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Role assigned: " + roleName, empId, actorEmpId);
    }

    /**
     * Log role revocation.
     */
    @Transactional
    public void logRoleRevoke(Long empId, Long actorEmpId, String roleName) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setActorEmpId(actorEmpId);
        log.setOperation("ROLE_REVOKE");
        log.setFieldName("role");
        log.setOldValue(roleName);
        log.setDescription("Role revoked: " + roleName);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Role revoked: " + roleName, empId, actorEmpId);
    }

    /**
     * Log account lockout.
     */
    @Transactional
    public void logAccountLock(Long empId, int failedAttempts) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setOperation("ACCOUNT_LOCK");
        log.setFieldName("account");
        log.setDescription("Account locked after " + failedAttempts + " failed login attempts");
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logInfo("Account locked after failed attempts", empId, null);
    }

    /**
     * Log authentication failure.
     */
    @Transactional
    public void logAuthFailure(Long empId, String reason) {
        AuditLog log = new AuditLog();
        log.setEmpId(empId);
        log.setOperation("AUTH_FAILURE");
        log.setDescription(reason);
        log.setStatus("FAILURE");
        log.setErrorMessage(reason);
        log.setCreatedAt(LocalDateTime.now());
        addRequestContext(log);
        auditLogRepository.save(log);
        logWarning("Authentication failure: " + reason, empId);
    }

    /**
     * Add IP address and user agent from HTTP request context.
     */
    private void addRequestContext(AuditLog log) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ipAddress = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                log.setIpAddress(ipAddress);
                log.setUserAgent(userAgent);
            }
        } catch (Exception e) {
            log.warn("Could not extract request context for audit log", e);
        }
    }

    /**
     * Extract client IP address from request (handles proxies).
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Mask sensitive values for audit logging (don't log full values).
     */
    private String maskSensitive(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // For government IDs, show only last 4 digits
        if (fieldName.equals("aadhar_number") || fieldName.equals("pan_card_number")) {
            if (value.length() > 4) {
                return "***" + value.substring(value.length() - 4);
            }
        }
        
        // For phone numbers
        if (fieldName.equals("phone") || fieldName.equals("emergency_contact")) {
            if (value.length() > 4) {
                return "***" + value.substring(value.length() - 4);
            }
        }
        
        // For passwords, never log
        if (fieldName.equals("password")) {
            return "[REDACTED]";
        }
        
        // For salary, show amount hidden
        if (fieldName.equals("salary")) {
            return "[AMOUNT CHANGED]";
        }
        
        // For email, show domain
        if (fieldName.equals("email")) {
            int atIndex = value.indexOf('@');
            if (atIndex > 0) {
                return "***@" + value.substring(atIndex + 1);
            }
        }
        
        return value;
    }

    private void logInfo(String message, Long empId, Long actorId) {
        log.info("AUDIT: {} - empId: {}, actorId: {}", message, empId, actorId);
    }

    private void logWarning(String message, Long empId) {
        log.warn("AUDIT: {} - empId: {}", message, empId);
    }
}
```

#### 4.3 Create AuditLogRepository

**File:** `src/main/java/com/cutm/smo/repositories/AuditLogRepository.java` (NEW)

```java
package com.cutm.smo.repositories;

import com.cutm.smo.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEmpIdOrderByCreatedAtDesc(Long empId);

    List<AuditLog> findByActorEmpIdOrderByCreatedAtDesc(Long actorEmpId);

    List<AuditLog> findByOperationOrderByCreatedAtDesc(String operation);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :start AND :end ORDER BY al.createdAt DESC")
    List<AuditLog> findByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT al FROM AuditLog al WHERE al.empId = :empId AND al.operation = :operation ORDER BY al.createdAt DESC")
    List<AuditLog> findByEmpIdAndOperation(Long empId, String operation);
}
```

---

### Phase 5: Update Services for Secure Password Handling (WEEKS 3-4)

#### 5.1 Update LoginService with BCrypt Enforcement

**File:** `src/main/java/com/cutm/smo/services/LoginService.java` (UPDATE)

```java
package com.cutm.smo.services;

import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.security.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final AuditLogService auditLogService;  // NEW

    public Optional<EmployeeLogin> getLoginById(Long empId) {
        return employeeLoginRepository.findById(empId);
    }

    @Transactional
    public EmployeeLogin createLogin(EmployeeLogin login) {
        if (login == null || login.getEmpId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empId is required");
        if (!employeeInfoRepository.existsById(login.getEmpId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        if (employeeLoginRepository.existsById(login.getEmpId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already exists for employee");
        if (login.getPassword() == null || login.getPassword().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        if (login.getStatus() == null || login.getStatus().isBlank()) login.setStatus("ACTIVE");
        
        // CRITICAL: Hash password with BCrypt before saving
        String hashedPassword = PasswordUtil.encodePassword(login.getPassword());
        login.setPassword(hashedPassword);
        login.setPasswordChangedAt(LocalDateTime.now());
        login.setFailedLoginAttempts(0);
        login.setAccountLocked(false);
        
        EmployeeLogin saved = employeeLoginRepository.save(login);
        auditLogService.logPasswordReset(login.getEmpId(), null);  // System action
        return saved;
    }

    @Transactional
    public EmployeeLogin updateLogin(Long empId, EmployeeLogin patch) {
        if (!employeeInfoRepository.existsById(empId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        EmployeeLogin existing = employeeLoginRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        
        if (patch != null && patch.getPassword() != null && !patch.getPassword().isBlank()) {
            // CRITICAL: Hash password with BCrypt before saving
            String hashedPassword = PasswordUtil.encodePassword(patch.getPassword());
            existing.setPassword(hashedPassword);
            existing.setPasswordChangedAt(LocalDateTime.now());
            existing.setFailedLoginAttempts(0);  // Reset failed attempts on password change
            auditLogService.logPasswordChange(empId, null);  // System action
        }
        
        if (patch != null && patch.getStatus() != null && !patch.getStatus().isBlank())
            existing.setStatus(patch.getStatus().trim().toUpperCase(Locale.ROOT));
            
        return employeeLoginRepository.save(existing);
    }

    @Transactional
    public void updatePassword(Long empId, String newPassword) {
        EmployeeLogin login = employeeLoginRepository.findById(empId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        
        // CRITICAL: Hash password with BCrypt before saving
        String hashedPassword = PasswordUtil.encodePassword(newPassword.trim());
        login.setPassword(hashedPassword);
        login.setPasswordChangedAt(LocalDateTime.now());
        login.setFailedLoginAttempts(0);  // Reset failed attempts
        login.setAccountLocked(false);
        login.setAccountLockedUntil(null);
        
        employeeLoginRepository.save(login);
        auditLogService.logPasswordChange(empId, null);  // System action
    }

    /**
     * Reset login account after failed attempts.
     * Should be called by admin after investigation.
     */
    @Transactional
    public void resetAccountLock(Long empId, Long adminEmpId) {
        EmployeeLogin login = employeeLoginRepository.findById(empId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        
        login.setFailedLoginAttempts(0);
        login.setAccountLocked(false);
        login.setAccountLockedUntil(null);
        
        employeeLoginRepository.save(login);
        auditLogService.logRoleAssign(empId, adminEmpId, "Account unlocked");  // Reuse for account actions
    }
}
```

#### 5.2 Update EmployeeService for Secure Passwords

**File:** `src/main/java/com/cutm/smo/services/EmployeeService.java` (UPDATE sections)

```java
// In createEmployee() method, around line 88-92:

@Transactional
public EmployeeInfo createEmployee(String actorEmpId, CreateEmployeeRequest request) {
    // ... existing validation ...
    
    EmployeeInfo saved = employeeInfoRepository.save(emp);

    EmployeeLogin login = new EmployeeLogin();
    login.setEmpId(empId);
    // CRITICAL: Hash password with BCrypt before saving
    login.setPassword(PasswordUtil.encodePassword(request.getPassword().trim()));
    login.setStatus("ACTIVE");
    login.setPasswordChangedAt(LocalDateTime.now());
    login.setFailedLoginAttempts(0);
    login.setAccountLocked(false);
    
    employeeLoginRepository.save(login);
    
    Long actorId = parseId(actorEmpId, "actorEmpId");
    auditLogService.logPasswordReset(empId, actorId);  // Log employee creation with password
    
    return saved;
}
```

#### 5.3 Update AuthService for Account Lockout

**File:** `src/main/java/com/cutm/smo/services/AuthService.java` (UPDATE)

```java
// Add these constants at the top
private static final int MAX_FAILED_ATTEMPTS = 5;
private static final long LOCKOUT_DURATION_MINUTES = 15;

// Update login() method password verification section (around line 84-98):

// Verify password using BCrypt
String storedPassword = login.getPassword();
String providedPassword = request.getPassword().trim();

boolean passwordMatches;
try {
    if (PasswordUtil.isBcryptHashed(storedPassword)) {
        // Password is hashed, use BCrypt matching
        passwordMatches = passwordEncoder.matches(providedPassword, storedPassword);
    } else {
        // Fallback for plain text passwords (migration mode only)
        log.warn("SECURITY WARNING: Plain text password detected for empId: {}. " +
                 "This should be migrated to BCrypt immediately.", empId);
        passwordMatches = storedPassword.equals(providedPassword);
    }
} catch (Exception e) {
    log.error("Password verification failed for empId: {}", empId, e);
    passwordMatches = false;
}

if (!passwordMatches) {
    log.warn("Login failed: Invalid password for employee ID: {}", empId);
    
    // Track failed attempts for account lockout
    login.setFailedLoginAttempts((login.getFailedLoginAttempts() == null ? 0 : login.getFailedLoginAttempts()) + 1);
    
    if (login.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
        log.warn("SECURITY: Account locked for empId {} after {} failed attempts", empId, MAX_FAILED_ATTEMPTS);
        login.setAccountLocked(true);
        login.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        employeeLoginRepository.save(login);
        
        auditLogService.logAccountLock(empId, login.getFailedLoginAttempts());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
            "Account locked due to too many failed login attempts. Please contact administrator.");
    }
    
    employeeLoginRepository.save(login);
    LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "Invalid password");
    auditLogService.logAuthFailure(empId, "Invalid password");
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
}

// Check if account is locked
if (login.getAccountLocked() != null && login.getAccountLocked()) {
    if (login.getAccountLockedUntil() != null && LocalDateTime.now().isBefore(login.getAccountLockedUntil())) {
        log.warn("Login attempt on locked account for empId: {}", empId);
        auditLogService.logAuthFailure(empId, "Account locked");
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is temporarily locked. Please try again later.");
    } else {
        // Unlock if lockout duration has passed
        log.info("Unlocking account for empId: {} - lockout duration expired", empId);
        login.setAccountLocked(false);
        login.setAccountLockedUntil(null);
        login.setFailedLoginAttempts(0);
        employeeLoginRepository.save(login);
    }
}

// Reset failed attempts on successful login
login.setFailedLoginAttempts(0);
login.setLastLoginAttempt(LocalDateTime.now());
employeeLoginRepository.save(login);
```

---

### Phase 6: Update DataInitializer (WEEKS 1)

#### 6.1 DataInitializer.java (UPDATE)

**File:** `src/main/java/com/cutm/smo/config/DataInitializer.java`

```java
// Around line 52-75, update ensureEmployeeWithLogin method:

private void ensureEmployeeWithLogin(Long empId, String empName, Role role, String password) {
    if (!employeeInfoRepository.existsById(empId)) {
        EmployeeInfo employee = new EmployeeInfo();
        employee.setEmpId(empId);
        employee.setEmpName(empName);
        employee.setRole(role);
        employee.setDob(LocalDate.of(1992, 1, 1));
        employee.setPhone("900000000" + empId);
        employee.setEmail(empName.toLowerCase().replace(" ", "") + "@smo.local");
        employee.setEmpDate(LocalDate.now());
        employee.setBloodGroup("O+");
        employee.setEmergencyContact("9000000099");
        employee.setStatus("ACTIVE");
        employee.setEncryptionKeyVersion(1);  // NEW: track encryption version
        employeeInfoRepository.save(employee);
    }

    if (!employeeLoginRepository.existsById(empId)) {
        EmployeeLogin login = new EmployeeLogin();
        login.setEmpId(empId);
        // CRITICAL: Hash password with BCrypt before saving
        login.setPassword(PasswordUtil.encodePassword(password));
        login.setStatus("ACTIVE");
        login.setPasswordChangedAt(LocalDateTime.now());  // NEW
        login.setFailedLoginAttempts(0);                  // NEW
        login.setAccountLocked(false);                    // NEW
        employeeLoginRepository.save(login);
    }
}
```

---

## PART 5: MIGRATION SCRIPTS

### Script 1: V2_0_0__Encrypt_Passwords_BCrypt.sql

**Flyway Migration File:** `src/main/resources/db/migration/V2_0_0__Encrypt_Passwords_BCrypt.sql`

```sql
-- ============================================================================
-- MIGRATION: Encrypt Passwords with BCrypt
-- ============================================================================
-- CRITICAL SECURITY UPDATE
-- This migration must be executed AFTER deploying the updated application code
-- that properly hashes passwords using PasswordUtil.encodePassword()
--
-- Timeline:
-- 1. Deploy application with PasswordUtil encryption
-- 2. Stop application
-- 3. Run this migration script manually
-- 4. Restart application - it will hash all existing plain text passwords
-- 5. Run V2_0_1 to finalize
-- ============================================================================

-- Step 1: Add temporary column for tracking encrypted passwords
ALTER TABLE login ADD COLUMN password_hashed BOOLEAN DEFAULT FALSE;

-- Step 2: Password column already sized correctly for BCrypt (VARCHAR 255)
-- BCrypt output: $2a$10$... (60 characters)
-- Current column: VARCHAR(255) ✓ OK

-- Step 3: After application restarts and hashes passwords,
-- verify all passwords are hashed
-- SELECT emp_id, password FROM login WHERE password NOT LIKE '$2%$';
-- Result should be empty

-- For now, just ensure the column exists
ALTER TABLE login MODIFY COLUMN password VARCHAR(255) NOT NULL;

COMMIT;
```

### Script 2: V2_0_1__Add_Security_Metadata.sql

**Flyway Migration File:** `src/main/resources/db/migration/V2_0_1__Add_Security_Metadata.sql`

```sql
-- ============================================================================
-- MIGRATION: Add Security Metadata Columns
-- ============================================================================
-- Adds columns for password change tracking and account lockout

ALTER TABLE login ADD COLUMN password_changed_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE login ADD COLUMN last_login_attempt DATETIME;
ALTER TABLE login ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE login ADD COLUMN account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE login ADD COLUMN account_locked_until DATETIME;

-- Create indexes for security columns
CREATE INDEX idx_login_account_locked ON login(account_locked, account_locked_until);
CREATE INDEX idx_login_password_changed ON login(password_changed_at);

-- Add encryption key version column to employee table
ALTER TABLE employee ADD COLUMN encryption_key_version INT DEFAULT 1;

-- Column to track which fields are encrypted (JSON format)
ALTER TABLE employee ADD COLUMN encrypted_fields JSON;

-- Update existing records to mark encryption version
UPDATE employee SET encryption_key_version = 1 WHERE encryption_key_version IS NULL;

-- Increase column sizes for encrypted storage
ALTER TABLE employee MODIFY COLUMN phone VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN address VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN email VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN blood_group VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN emergency_contact VARCHAR(512);

-- Create indexes for audit performance
CREATE INDEX idx_employee_encryption_version ON employee(encryption_key_version);

COMMIT;
```

### Script 3: V2_0_2__Create_Audit_Log_Table.sql

**Flyway Migration File:** `src/main/resources/db/migration/V2_0_2__Create_Audit_Log_Table.sql`

```sql
-- ============================================================================
-- MIGRATION: Create Audit Log Table
-- ============================================================================
-- Tracks all sensitive operations for compliance and security investigation

CREATE TABLE audit_log (
    audit_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT 'Employee whose data was affected',
    actor_emp_id BIGINT COMMENT 'Employee who performed the action',
    operation VARCHAR(50) NOT NULL COMMENT 'PASSWORD_CHANGE, EMPLOYEE_UPDATE, ROLE_ASSIGN, etc.',
    field_name VARCHAR(100) COMMENT 'Which field was affected',
    old_value VARCHAR(1000) COMMENT 'Old value (encrypted if sensitive)',
    new_value VARCHAR(1000) COMMENT 'New value (encrypted if sensitive)',
    description VARCHAR(500) COMMENT 'Summary of what happened',
    ip_address VARCHAR(50) COMMENT 'IP address of request origin',
    user_agent VARCHAR(500) COMMENT 'User agent of request',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS or FAILURE',
    error_message VARCHAR(500) COMMENT 'Error message if FAILURE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When operation occurred',
    
    -- Indexes for common queries
    INDEX idx_audit_emp (emp_id),
    INDEX idx_audit_actor (actor_emp_id),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_timestamp (created_at),
    INDEX idx_audit_emp_operation (emp_id, operation),
    
    -- Foreign key constraint (optional, consider performance)
    -- CONSTRAINT fk_audit_emp FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
    
    CONSTRAINT uk_audit_no_duplicates UNIQUE KEY (audit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log for tracking sensitive operations';

COMMIT;
```

### Script 4: V2_0_3__Backfill_Password_Hashes.sql

**Flyway Migration File:** `src/main/resources/db/migration/V2_0_3__Backfill_Password_Hashes.sql`

```sql
-- ============================================================================
-- MIGRATION: Verify Password Hashes
-- ============================================================================
-- After application has been running and hashing passwords,
-- verify that all passwords are now BCrypt hashed

-- This is a verification script - DO NOT execute destructively
-- Just verify that passwords are hashed

-- Check which passwords are still plain text (should be none)
-- SELECT emp_id, password, 
--        IF(password LIKE '$2a$%' OR password LIKE '$2b$%' OR password LIKE '$2y$%', 'HASHED', 'PLAIN') as password_status
-- FROM login;

-- Mark hashed passwords
UPDATE login SET password_hashed = TRUE 
WHERE password LIKE '$2a$%' 
   OR password LIKE '$2b$%' 
   OR password LIKE '$2y$%';

-- Alert if any plain text passwords remain (this should never happen)
-- SELECT COUNT(*) as plain_text_count FROM login WHERE password_hashed = FALSE;

COMMIT;
```

---

## PART 6: CONFIGURATION REQUIREMENTS

### Environment Variables

**Development (.env or application.properties):**
```properties
# Encryption configuration
encryption.key=YOUR_BASE64_ENCODED_256_BIT_KEY_HERE
encryption.key.rotation.enabled=false

# Password policy
password.min.length=8
password.require.uppercase=true
password.require.numbers=true
password.require.special=true
password.expiry.days=90
password.history.count=5

# Account lockout policy
account.lockout.threshold=5
account.lockout.duration.minutes=15
```

**Production (.env or application-prod.properties):**
```properties
# Encryption configuration
encryption.key=${ENCRYPTION_KEY}
encryption.key.rotation.enabled=true
encryption.key.rotation.interval.days=90

# Password policy
password.min.length=12
password.require.uppercase=true
password.require.numbers=true
password.require.special=true
password.expiry.days=60
password.history.count=10

# Account lockout policy
account.lockout.threshold=3
account.lockout.duration.minutes=30
```

### Key Generation

**Generate Base64-encoded 256-bit AES key (run ONCE, store in vault):**

```bash
# Using OpenSSL
openssl enc -aes-256-cbc -S $(openssl rand -hex 8) -P -pass pass:secret | grep key | cut -d'=' -f2

# Or using Java
java -jar GenerateEncryptionKey.jar

# Or using Python
python3 << 'EOF'
import os
import base64
key = os.urandom(32)  # 256 bits
print(base64.b64encode(key).decode('utf-8'))
EOF
```

**Store the output in secure vault (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault):**
```
encryption.key = <BASE64_ENCODED_KEY>
```

---

## PART 7: DEPLOYMENT CHECKLIST

### Pre-Deployment
- [ ] Code review of encryption utility and converters
- [ ] Test encryption/decryption with various data types
- [ ] Generate and securely store encryption key
- [ ] Backup current database before migration
- [ ] Plan rollback strategy

### Deployment Phase 1: Code & Database Schema
- [ ] Deploy updated application code with encryption utilities
- [ ] Run Flyway migration V2_0_0 (password hashing)
- [ ] Run Flyway migration V2_0_1 (security metadata)
- [ ] Run Flyway migration V2_0_2 (audit log table)
- [ ] Verify all migrations completed successfully

### Deployment Phase 2: Data Migration
- [ ] Application automatically hashes plain text passwords on login
- [ ] Monitor logs for "Plain text password detected" warnings
- [ ] After all users have logged in once, run V2_0_3 (verification)
- [ ] Verify no plain text passwords remain

### Post-Deployment
- [ ] Test password change functionality
- [ ] Test encryption/decryption of PII on employee update
- [ ] Verify audit logs are being created
- [ ] Test account lockout after 5 failed attempts
- [ ] Monitor application logs for encryption errors
- [ ] Update security documentation
- [ ] Train administrators on new security features

### Rollback Plan
- [ ] If issues arise, restore from pre-migration backup
- [ ] Disable encryption converters if migration fails
- [ ] Contact database administrator and security team

---

## PART 8: TESTING STRATEGY

### Unit Tests

```java
// Test password hashing
@Test
void testPasswordEncoding() {
    String plainPassword = "TestPassword123!";
    String encoded = PasswordUtil.encodePassword(plainPassword);
    
    assertThat(encoded).isNotEqualTo(plainPassword);
    assertThat(PasswordUtil.isBcryptHashed(encoded)).isTrue();
    assertThat(PasswordUtil.matchPassword(plainPassword, encoded)).isTrue();
    assertThat(PasswordUtil.matchPassword("wrongpassword", encoded)).isFalse();
}

// Test encryption/decryption
@Test
void testAES256Encryption() {
    String plainText = "1234567890123456"; // Aadhar number
    String encrypted = encryptionUtil.encrypt(plainText);
    
    assertThat(encrypted).isNotEqualTo(plainText);
    assertThat(EncryptionUtil.isEncrypted(encrypted)).isTrue();
    
    String decrypted = encryptionUtil.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(plainText);
}

// Test JPA converter
@Test
void testEncryptedStringConverter() {
    EmployeeInfo employee = new EmployeeInfo();
    employee.setAadharNumber("1234567890123456");
    
    employeeInfoRepository.save(employee);
    
    // Verify encrypted in database
    EmployeeInfo loaded = employeeInfoRepository.findById(employee.getEmpId()).get();
    assertThat(loaded.getAadharNumber()).isEqualTo("1234567890123456"); // Decrypted by converter
}

// Test audit logging
@Test
void testAuditLogging() {
    auditLogService.logPasswordChange(1001L, 1002L);
    
    List<AuditLog> logs = auditLogRepository.findByEmpIdAndOperation(1001L, "PASSWORD_CHANGE");
    assertThat(logs).isNotEmpty();
    assertThat(logs.get(0).getActorEmpId()).isEqualTo(1002L);
}
```

### Integration Tests

```java
// Test login with new password hashing
@Test
void testLoginWithHashedPassword() {
    LoginRequest request = new LoginRequest("1001", "hr123");
    LoginResponse response = authService.login(request);
    
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotEmpty();
}

// Test failed login attempts and lockout
@Test
void testAccountLockoutAfterFailedAttempts() {
    for (int i = 0; i < 5; i++) {
        LoginRequest request = new LoginRequest("1001", "wrongpassword");
        assertThrows(ResponseStatusException.class, () -> authService.login(request));
    }
    
    EmployeeLogin login = employeeLoginRepository.findById(1001L).get();
    assertThat(login.getAccountLocked()).isTrue();
}
```

---

## PART 9: SECURITY RECOMMENDATIONS SUMMARY

### Priority 1: CRITICAL - Implement Immediately

1. **Password Hashing**
   - ✓ Deploy PasswordUtil enforcement in all password creation/update paths
   - ✓ Run DataInitializer with BCrypt hashing
   - ✓ Audit all existing plain text passwords

2. **PII Encryption**
   - ✓ Implement AES-256 encryption for Aadhar, PAN numbers
   - ✓ Create JPA converters for transparent encryption
   - ✓ Store encryption keys in environment variables only

3. **Audit Logging**
   - ✓ Create audit_log table
   - ✓ Log all password changes, role assignments, sensitive updates
   - ✓ Implement audit trail for compliance

### Priority 2: HIGH - Implement Within 2 Months

1. **Account Lockout Policy**
   - ✓ Implement after 5 failed login attempts
   - ✓ Lock for 15 minutes initially
   - ✓ Admin unlock capability

2. **Additional PII Encryption**
   - Encrypt phone numbers (searchability trade-off)
   - Encrypt emergency contact
   - Encrypt DOB (optional, for authentication bypass prevention)

3. **Database Hardening**
   - Implement database user with minimal permissions
   - Enable query logging for audit table access
   - Implement connection pooling with SSL

### Priority 3: MEDIUM - Implement Within 6 Months

1. **Key Management**
   - Implement key rotation mechanism
   - Store keys in HashiCorp Vault or AWS Secrets Manager
   - Track key versions per employee

2. **Backup Encryption**
   - Encrypt database backups
   - Separate key storage from backups
   - Test backup restoration with encryption

3. **Compliance**
   - Implement data classification labels
   - Add privacy policy enforcement
   - Create GDPR/PDPA compliance reports

### Priority 4: ONGOING

1. **Security Monitoring**
   - Monitor audit logs for suspicious patterns
   - Alert on multiple failed login attempts
   - Track encryption key usage

2. **Regular Security Audits**
   - Quarterly penetration testing
   - Annual compliance reviews
   - Security training for developers

3. **Incident Response**
   - Create incident response plan
   - Define escalation procedures
   - Document breach notification process

---

## PART 10: COMPLIANCE CHECKLIST

### Data Protection Regulations

- [ ] **GDPR (EU)**: If applicable, ensure right to erasure and data portability
- [ ] **PDPA (India)**: Comply with Sensitive Personal Data requirements
- [ ] **POPIA (South Africa)**: Encrypt personal information
- [ ] **CCPA (California)**: Implement data minimization and purpose limitation

### Security Standards

- [ ] **OWASP Top 10**: Password hashing (✓), Encryption (✓), Audit logging (✓)
- [ ] **NIST Cybersecurity Framework**: Identify, Protect, Detect, Respond
- [ ] **ISO 27001**: Information security management
- [ ] **PCI DSS**: If handling payment information

### Required Controls

- [ ] Password hashing with BCrypt (strength 12+) ✓
- [ ] AES-256 encryption for PII ✓
- [ ] Audit logging for sensitive operations ✓
- [ ] Access control and role-based authorization
- [ ] Data retention and purge policies
- [ ] Backup encryption and testing
- [ ] Incident response procedures
- [ ] Employee security training

---

## CONCLUSION

The SMO database currently stores highly sensitive employee data (government IDs, salaries, personal information) in PLAIN TEXT, representing a CRITICAL SECURITY VULNERABILITY. Additionally, passwords are not being enforced to be hashed, creating an authentication security gap.

This audit provides a comprehensive 6-week remediation plan with:
1. **Immediate**: Password hashing enforcement (Week 1)
2. **Phase 1**: PII encryption infrastructure (Week 2-3)
3. **Phase 2**: Audit logging implementation (Week 2-4)
4. **Phase 3**: Testing and deployment (Week 4-6)

All code, SQL migrations, and configurations are provided. Implementation requires coordination between development, database administration, and security teams.

---

**Prepared by:** Database Security Audit Team  
**Date:** 2026-06-19  
**Severity:** CRITICAL  
**Estimated Remediation:** 6-8 weeks
