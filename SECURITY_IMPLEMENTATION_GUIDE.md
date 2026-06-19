# Security Implementation Guide - Code Ready for Use

This guide provides ready-to-use code snippets for implementing database security improvements. Copy-paste these directly into your codebase.

---

## PHASE 1: SETUP (Week 1)

### 1.1 Create EncryptionUtil.java

**Location:** `src/main/java/com/cutm/smo/security/EncryptionUtil.java`

See FULL CODE in: `DATABASE_SECURITY_AUDIT.md` Part 4.2.1

**Key Points:**
- AES-256 encryption/decryption
- Requires `encryption.key` environment variable (Base64-encoded 256-bit key)
- Throws exception if key not configured
- Automatic encryption key generation helper

### 1.2 Create EncryptedStringConverter.java

**Location:** `src/main/java/com/cutm/smo/security/EncryptedStringConverter.java`

See FULL CODE in: `DATABASE_SECURITY_AUDIT.md` Part 4.2.2

**Key Points:**
- JPA AttributeConverter for transparent encryption
- Use `@Convert(converter = EncryptedStringConverter.class)` on entity fields
- Handles null values gracefully
- Prevents double-encryption of already encrypted data

### 1.3 Generate Encryption Key

**Run ONCE to generate key (execute in your terminal):**

```bash
# Option 1: Using OpenSSL (Linux/Mac/Git Bash)
openssl enc -aes-256-cbc -S $(openssl rand -hex 8) -P -pass pass:secret | grep key | awk '{print $NF}' | xxd -r -p | base64

# Option 2: Using Python 3
python3 << 'EOF'
import os
import base64
key = os.urandom(32)  # 256 bits = 32 bytes
print(base64.b64encode(key).decode('utf-8'))
EOF

# Option 3: Using Java (create GenerateKey.java)
java GenerateKey
```

**Output:** A Base64 string like `K7uXYz5...` (about 44 characters)

**Store securely:**
- AWS Secrets Manager: `smo/encryption/key`
- Azure Key Vault: `smo-encryption-key`
- HashiCorp Vault: `secret/smo/encryption/key`
- Environment variable: `ENCRYPTION_KEY`

**NEVER commit to Git or hardcode in properties files**

---

## PHASE 2: UPDATE MODELS (Week 2)

### 2.1 Update EmployeeLogin.java

**File:** `src/main/java/com/cutm/smo/models/EmployeeLogin.java`

**Changes (FULL REPLACEMENT):**

```java
package com.cutm.smo.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(exclude = "employee")
@ToString(exclude = "employee")
@Entity
@Table(name = "login")
public class EmployeeLogin {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", insertable = false, updatable = false)
    private EmployeeInfo employee;

    @Column(name = "password", nullable = false, length = 255)
    private String password;  // Must be BCrypt hashed

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    // NEW SECURITY COLUMNS
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
}
```

### 2.2 Update EmployeeInfo.java

**File:** `src/main/java/com/cutm/smo/models/EmployeeInfo.java`

**Changes (REPLACEMENT):**

```java
package com.cutm.smo.models;

import com.cutm.smo.security.EncryptedStringConverter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(exclude = "logins")
@ToString(exclude = "logins")
@Entity
@Table(
        name = "employee",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_phone", columnNames = "phone"),
                @UniqueConstraint(name = "uk_employee_email", columnNames = "email")
        }
)
public class EmployeeInfo {

    @Id
    @Column(name = "emp_id", nullable = false)
    private Long empId;

    @Column(name = "name", nullable = false, length = 150)
    private String empName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @JsonManagedReference
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EmployeeLogin> logins;

    @Column(name = "dob")
    @Convert(converter = EncryptedStringConverter.class)
    private LocalDate dob;

    @Column(name = "phone", length = 512)  // CHANGED: increased for encrypted storage
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;

    @Column(name = "address", length = 512)  // CHANGED: increased
    @Convert(converter = EncryptedStringConverter.class)
    private String address;

    @Column(name = "email", nullable = false, length = 512)  // CHANGED: increased
    private String email;  // Consider: encrypt or keep plaintext for searchability

    @Column(name = "salary", length = 512)  // CHANGED: VARCHAR instead of DECIMAL
    @Convert(converter = EncryptedStringConverter.class)
    private String salary;  // CHANGED: String for encryption capability

    @Column(name = "emp_date", nullable = false)
    private LocalDate empDate;

    @Column(name = "blood_group", length = 512)  // CHANGED: increased
    @Convert(converter = EncryptedStringConverter.class)
    private String bloodGroup;

    @Column(name = "emergency_contact", length = 512)  // CHANGED: increased
    @Convert(converter = EncryptedStringConverter.class)
    private String emergencyContact;

    @Column(name = "aadhar_number", length = 512)
    @Convert(converter = EncryptedStringConverter.class)
    private String aadharNumber;

    @Column(name = "pan_card_number", length = 512)
    @Convert(converter = EncryptedStringConverter.class)
    private String panCardNumber;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // NEW SECURITY COLUMNS
    @Column(name = "encryption_key_version", nullable = false)
    private Integer encryptionKeyVersion = 1;

    @Column(name = "encrypted_fields")
    private String encryptedFields;  // JSON tracking

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}
```

### 2.3 Create AuditLog.java

**Location:** `src/main/java/com/cutm/smo/models/AuditLog.java`

See FULL CODE in: `DATABASE_SECURITY_AUDIT.md` Part 4.4.1

---

## PHASE 3: UPDATE SERVICES (Week 3)

### 3.1 Update LoginService.java - ENFORCE BCrypt

**File:** `src/main/java/com/cutm/smo/services/LoginService.java`

**Key Changes:**

```java
package com.cutm.smo.services;

import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.security.PasswordUtil;  // NEW: import
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // NEW: import
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;  // NEW: import
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final AuditLogService auditLogService;  // NEW: inject

    public Optional<EmployeeLogin> getLoginById(Long empId) {
        return employeeLoginRepository.findById(empId);
    }

    @Transactional  // NEW: add
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
        
        // NEW: Hash password with BCrypt before saving (CRITICAL!)
        String hashedPassword = PasswordUtil.encodePassword(login.getPassword());
        login.setPassword(hashedPassword);
        login.setPasswordChangedAt(LocalDateTime.now());  // NEW
        login.setFailedLoginAttempts(0);  // NEW
        login.setAccountLocked(false);  // NEW
        
        EmployeeLogin saved = employeeLoginRepository.save(login);
        auditLogService.logPasswordReset(login.getEmpId(), null);  // NEW: log
        return saved;
    }

    @Transactional  // NEW: add
    public EmployeeLogin updateLogin(Long empId, EmployeeLogin patch) {
        if (!employeeInfoRepository.existsById(empId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        EmployeeLogin existing = employeeLoginRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        if (patch != null && patch.getPassword() != null && !patch.getPassword().isBlank()) {
            // NEW: Hash password with BCrypt before saving (CRITICAL!)
            String hashedPassword = PasswordUtil.encodePassword(patch.getPassword());
            existing.setPassword(hashedPassword);
            existing.setPasswordChangedAt(LocalDateTime.now());  // NEW
            existing.setFailedLoginAttempts(0);  // NEW: reset on password change
            auditLogService.logPasswordChange(empId, null);  // NEW: log
        }
        if (patch != null && patch.getStatus() != null && !patch.getStatus().isBlank())
            existing.setStatus(patch.getStatus().trim().toUpperCase(Locale.ROOT));
        return employeeLoginRepository.save(existing);
    }

    @Transactional  // NEW: add
    public void updatePassword(Long empId, String newPassword) {
        EmployeeLogin login = employeeLoginRepository.findById(empId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        
        // NEW: Hash password with BCrypt before saving (CRITICAL!)
        String hashedPassword = PasswordUtil.encodePassword(newPassword.trim());
        login.setPassword(hashedPassword);
        login.setPasswordChangedAt(LocalDateTime.now());  // NEW
        login.setFailedLoginAttempts(0);  // NEW: reset on password change
        login.setAccountLocked(false);  // NEW: unlock on password change
        login.setAccountLockedUntil(null);  // NEW
        
        employeeLoginRepository.save(login);
        auditLogService.logPasswordChange(empId, null);  // NEW: log
    }

    // NEW: Add method for admin to unlock account
    @Transactional
    public void resetAccountLock(Long empId, Long adminEmpId) {
        EmployeeLogin login = employeeLoginRepository.findById(empId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        
        login.setFailedLoginAttempts(0);
        login.setAccountLocked(false);
        login.setAccountLockedUntil(null);
        
        employeeLoginRepository.save(login);
        auditLogService.logRoleAssign(empId, adminEmpId, "Account unlocked");
    }
}
```

### 3.2 Update EmployeeService.java - ENFORCE BCrypt

**File:** `src/main/java/com/cutm/smo/services/EmployeeService.java`

**Location:** Lines 49-94 (createEmployee method)

**Change From:**
```java
EmployeeLogin login = new EmployeeLogin();
login.setEmpId(empId);
login.setPassword(request.getPassword().trim());  // WRONG: plain text!
login.setStatus("ACTIVE");
employeeLoginRepository.save(login);
```

**Change To:**
```java
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
auditLogService.logPasswordReset(empId, actorId);  // Log creation
```

**Imports to add:**
```java
import com.cutm.smo.security.PasswordUtil;
import com.cutm.smo.services.AuditLogService;
import java.time.LocalDateTime;
```

**Injection to add (in constructor):**
```java
private final AuditLogService auditLogService;
```

### 3.3 Update DataInitializer.java - ENFORCE BCrypt

**File:** `src/main/java/com/cutm/smo/config/DataInitializer.java`

**Location:** Lines 52-75 (ensureEmployeeWithLogin method)

**Change From:**
```java
private void ensureEmployeeWithLogin(Long empId, String empName, Role role, String password) {
    // ... employee creation ...
    
    if (!employeeLoginRepository.existsById(empId)) {
        EmployeeLogin login = new EmployeeLogin();
        login.setEmpId(empId);
        login.setPassword(password);  // WRONG: plain text!
        login.setStatus("ACTIVE");
        employeeLoginRepository.save(login);
    }
}
```

**Change To:**
```java
private void ensureEmployeeWithLogin(Long empId, String empName, Role role, String password) {
    // ... employee creation code unchanged ...
    
    if (!employeeLoginRepository.existsById(empId)) {
        EmployeeLogin login = new EmployeeLogin();
        login.setEmpId(empId);
        // CRITICAL: Hash password with BCrypt before saving
        login.setPassword(PasswordUtil.encodePassword(password));
        login.setStatus("ACTIVE");
        login.setPasswordChangedAt(LocalDateTime.now());
        login.setFailedLoginAttempts(0);
        login.setAccountLocked(false);
        employeeLoginRepository.save(login);
    }
}
```

**Imports to add:**
```java
import com.cutm.smo.security.PasswordUtil;
import java.time.LocalDateTime;
```

### 3.4 Update AuthService.java - ADD ACCOUNT LOCKOUT

**File:** `src/main/java/com/cutm/smo/services/AuthService.java`

**Location:** Top of class (add constants):
```java
private static final int MAX_FAILED_ATTEMPTS = 5;
private static final long LOCKOUT_DURATION_MINUTES = 15;
```

**Location:** Around lines 72-78, add check for locked account:
```java
// NEW: Check if account is locked
if (login.getAccountLocked() != null && login.getAccountLocked()) {
    if (login.getAccountLockedUntil() != null && LocalDateTime.now().isBefore(login.getAccountLockedUntil())) {
        log.warn("Login attempt on locked account for empId: {}", empId);
        auditLogService.logAuthFailure(empId, "Account locked");
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
            "Account is temporarily locked. Please try again later.");
    } else {
        // Unlock if lockout duration has passed
        log.info("Unlocking account for empId: {} - lockout duration expired", empId);
        login.setAccountLocked(false);
        login.setAccountLockedUntil(null);
        login.setFailedLoginAttempts(0);
        employeeLoginRepository.save(login);
    }
}
```

**Location:** Around lines 94-99, update password verification section:

**Replace:**
```java
if (!passwordMatches) {
    log.warn("Login failed: Invalid password for employee ID: {}", empId);
    LoggingUtil.logAuthenticationAttempt(log, request.getLoginid(), false, "Invalid password");
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
}
```

**With:**
```java
if (!passwordMatches) {
    log.warn("Login failed: Invalid password for employee ID: {}", empId);
    
    // NEW: Track failed attempts for account lockout
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
```

**Location:** Around line 99, add after password check passes:
```java
// NEW: Reset failed attempts on successful login
login.setFailedLoginAttempts(0);
login.setLastLoginAttempt(LocalDateTime.now());
employeeLoginRepository.save(login);
```

**Imports to add:**
```java
import com.cutm.smo.services.AuditLogService;
import java.time.LocalDateTime;
```

**Injection to add (in constructor):**
```java
private final AuditLogService auditLogService;
```

---

## PHASE 4: CREATE AUDIT SERVICES (Week 3-4)

### 4.1 Create AuditLogService.java

**Location:** `src/main/java/com/cutm/smo/services/AuditLogService.java`

See FULL CODE in: `DATABASE_SECURITY_AUDIT.md` Part 4.4.2

### 4.2 Create AuditLogRepository.java

**Location:** `src/main/java/com/cutm/smo/repositories/AuditLogRepository.java`

See FULL CODE in: `DATABASE_SECURITY_AUDIT.md` Part 4.4.3

---

## PHASE 5: DATABASE MIGRATIONS (Week 1)

### 5.1 Create V2_0_0__Encrypt_Passwords_BCrypt.sql

**File:** `src/main/resources/db/migration/V2_0_0__Encrypt_Passwords_BCrypt.sql`

```sql
-- ============================================================================
-- MIGRATION: Security Metadata for Password Hashing
-- ============================================================================

-- Step 1: Add flag to track encrypted passwords
ALTER TABLE login ADD COLUMN password_hashed BOOLEAN DEFAULT FALSE;

-- Verify existing column size (should be VARCHAR(255) - OK for BCrypt)
-- BCrypt hashes are exactly 60 characters: $2a$10$...(52 chars)...
-- Column size of 255 is more than sufficient

COMMIT;
```

### 5.2 Create V2_0_1__Add_Security_Metadata.sql

**File:** `src/main/resources/db/migration/V2_0_1__Add_Security_Metadata.sql`

```sql
-- ============================================================================
-- MIGRATION: Add Security Metadata Columns
-- ============================================================================

-- Add password change tracking
ALTER TABLE login ADD COLUMN password_changed_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE login ADD COLUMN last_login_attempt DATETIME;

-- Add failed attempt tracking for account lockout
ALTER TABLE login ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE login ADD COLUMN account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE login ADD COLUMN account_locked_until DATETIME;

-- Create indexes for security queries
CREATE INDEX idx_login_account_locked ON login(account_locked, account_locked_until);
CREATE INDEX idx_login_password_changed ON login(password_changed_at);

-- Add encryption metadata to employee table
ALTER TABLE employee ADD COLUMN encryption_key_version INT DEFAULT 1;
ALTER TABLE employee ADD COLUMN encrypted_fields JSON;

-- Increase column sizes for encrypted data (Base64 increases data by ~33%)
ALTER TABLE employee MODIFY COLUMN phone VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN address VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN email VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN blood_group VARCHAR(512);
ALTER TABLE employee MODIFY COLUMN emergency_contact VARCHAR(512);

-- Create index for encryption version tracking
CREATE INDEX idx_employee_encryption_version ON employee(encryption_key_version);

COMMIT;
```

### 5.3 Create V2_0_2__Create_Audit_Log_Table.sql

**File:** `src/main/resources/db/migration/V2_0_2__Create_Audit_Log_Table.sql`

```sql
-- ============================================================================
-- MIGRATION: Create Audit Log Table
-- ============================================================================

CREATE TABLE audit_log (
    audit_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT 'Employee whose data was affected',
    actor_emp_id BIGINT COMMENT 'Employee who performed the action',
    operation VARCHAR(50) NOT NULL COMMENT 'PASSWORD_CHANGE, EMPLOYEE_UPDATE, etc.',
    field_name VARCHAR(100) COMMENT 'Field affected',
    old_value VARCHAR(1000) COMMENT 'Old value (encrypted if sensitive)',
    new_value VARCHAR(1000) COMMENT 'New value (encrypted if sensitive)',
    description VARCHAR(500) COMMENT 'Summary',
    ip_address VARCHAR(50) COMMENT 'Request source IP',
    user_agent VARCHAR(500) COMMENT 'User agent',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS or FAILURE',
    error_message VARCHAR(500) COMMENT 'Error if failed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_audit_emp (emp_id),
    INDEX idx_audit_actor (actor_emp_id),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_timestamp (created_at),
    INDEX idx_audit_emp_operation (emp_id, operation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log for sensitive operations';

COMMIT;
```

---

## CONFIGURATION

### Update application.properties

**File:** `src/main/resources/application.properties`

**Add at the end:**

```properties
# ============================================================================
# SECURITY CONFIGURATION
# ============================================================================

# AES-256 Encryption Key (MUST be set as environment variable)
# Format: Base64-encoded 256-bit key (32 bytes)
# Generate with: python3 -c "import os, base64; print(base64.b64encode(os.urandom(32)).decode())"
# NEVER hardcode in properties - use environment variables only
encryption.key=${ENCRYPTION_KEY:}

# Password Policy
password.min.length=8
password.require.uppercase=true
password.require.numbers=true
password.require.special=true

# Account Lockout Policy
account.lockout.threshold=5
account.lockout.duration.minutes=15
```

### Set Environment Variable

**Development (Windows PowerShell):**
```powershell
$env:ENCRYPTION_KEY = "YOUR_BASE64_KEY_HERE"
```

**Development (Linux/Mac Bash):**
```bash
export ENCRYPTION_KEY="YOUR_BASE64_KEY_HERE"
```

**Production (Docker):**
```dockerfile
ENV ENCRYPTION_KEY=your-base64-key-from-secrets-manager
```

**Production (Kubernetes):**
```yaml
env:
  - name: ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: smo-secrets
        key: encryption-key
```

---

## TESTING

### Test 1: Password Hashing

```java
@Test
public void testPasswordHashing() {
    String plainPassword = "TestPassword123!";
    String hashedPassword = PasswordUtil.encodePassword(plainPassword);
    
    // Verify it's hashed
    assertThat(hashedPassword).isNotEqualTo(plainPassword);
    assertThat(PasswordUtil.isBcryptHashed(hashedPassword)).isTrue();
    
    // Verify matching works
    assertThat(PasswordUtil.matchPassword(plainPassword, hashedPassword)).isTrue();
    assertThat(PasswordUtil.matchPassword("wrongpassword", hashedPassword)).isFalse();
}
```

### Test 2: Encryption

```java
@Test
public void testEncryption() {
    String plainData = "1234567890123456";  // Aadhar number
    String encrypted = encryptionUtil.encrypt(plainData);
    
    assertThat(encrypted).isNotEqualTo(plainData);
    assertThat(EncryptionUtil.isEncrypted(encrypted)).isTrue();
    
    String decrypted = encryptionUtil.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(plainData);
}
```

### Test 3: Login with Hashed Password

```java
@Test
public void testLoginWithHashedPassword() {
    // Setup: Create employee with hashed password
    EmployeeInfo emp = new EmployeeInfo();
    emp.setEmpId(9999L);
    emp.setEmpName("Test User");
    emp.setRole(testRole);
    emp.setEmail("test@test.com");
    emp.setStatus("ACTIVE");
    emp.setEmpDate(LocalDate.now());
    employeeInfoRepository.save(emp);
    
    EmployeeLogin login = new EmployeeLogin();
    login.setEmpId(9999L);
    login.setPassword(PasswordUtil.encodePassword("TestPassword123!"));
    login.setStatus("ACTIVE");
    employeeLoginRepository.save(login);
    
    // Test: Login with correct password
    LoginRequest request = new LoginRequest("9999", "TestPassword123!");
    LoginResponse response = authService.login(request);
    
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isNotEmpty();
}
```

### Test 4: Account Lockout

```java
@Test
public void testAccountLockout() {
    // Attempt login 5 times with wrong password
    for (int i = 0; i < 5; i++) {
        LoginRequest request = new LoginRequest("1001", "wrongpassword");
        assertThrows(ResponseStatusException.class, () -> authService.login(request));
    }
    
    // Verify account is locked
    EmployeeLogin login = employeeLoginRepository.findById(1001L).get();
    assertThat(login.getAccountLocked()).isTrue();
    assertThat(login.getAccountLockedUntil()).isNotNull();
    
    // Verify correct password still fails due to lockout
    LoginRequest request = new LoginRequest("1001", "hr123");
    assertThrows(ResponseStatusException.class, () -> authService.login(request));
}
```

---

## DEPLOYMENT CHECKLIST

### Pre-Deployment
- [ ] Code review completed
- [ ] All tests passing
- [ ] Encryption key generated and stored securely
- [ ] Database backed up
- [ ] Rollback plan documented

### Deployment
- [ ] Deploy application code to development
- [ ] Run Flyway migrations V2_0_0, V2_0_1, V2_0_2
- [ ] Test password hashing with new logins
- [ ] Test account lockout functionality
- [ ] Monitor logs for encryption errors
- [ ] Verify audit logs are being created
- [ ] Deploy to staging environment
- [ ] Run full test suite
- [ ] Deploy to production (or schedule for maintenance window)

### Post-Deployment
- [ ] Monitor error logs for 24 hours
- [ ] Verify all passwords are BCrypt hashed
- [ ] Check audit log entries
- [ ] Test account lockout reset by admin
- [ ] Document any issues
- [ ] Update runbooks with new procedures

---

## TROUBLESHOOTING

### Issue: "encryption.key not configured"
**Solution:** Set `ENCRYPTION_KEY` environment variable with Base64-encoded 256-bit key

### Issue: "Decryption failed for sensitive field"
**Solution:** Data was encrypted with different key. Verify you're using correct key.

### Issue: "Password field too small"
**Solution:** Column should be VARCHAR(255). Check migration was applied.

### Issue: "Account locked indefinitely"
**Solution:** Lockout duration not calculated. Verify `account_locked_until` is set correctly.

### Issue: "Audit logs not being created"
**Solution:** Verify `AuditLogService` is autowired in calling service. Check for exceptions in logs.

---

## SECURITY BEST PRACTICES

1. **Never log passwords** - audit log masks passwords with [REDACTED]
2. **Never hardcode encryption keys** - always use environment variables
3. **Never commit secrets** - use `.gitignore` for sensitive files
4. **Rotate keys periodically** - implement key rotation within 6 months
5. **Test decryption** - verify backups can be decrypted before depending on them
6. **Monitor audit logs** - review for suspicious patterns weekly
7. **Update dependencies** - keep Spring Security and crypto libraries current

---

## Next Steps

1. Review full `DATABASE_SECURITY_AUDIT.md` for complete context
2. Generate encryption key and store securely
3. Copy code snippets to your IDE
4. Create feature branch: `feature/security-encryption`
5. Implement Phase 1 (password hashing) first
6. Deploy to development and test thoroughly
7. Proceed to Phase 2-4 (encryption and audit logging)
8. Plan production deployment

---

**Created:** 2026-06-19  
**Version:** 1.0  
**Status:** Ready for Implementation
