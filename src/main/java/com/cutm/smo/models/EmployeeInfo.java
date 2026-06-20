package com.cutm.smo.models;

import com.cutm.smo.util.EncryptedStringConverter;
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
    private LocalDate dob;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "emp_date", nullable = false)
    private LocalDate empDate;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "aadhar_number", length = 512)
    private String aadharNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pan_card_number", length = 512)
    private String panCardNumber;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "encryption_key_version")
    private Integer encryptionKeyVersion = 1;

    // ENCRYPTED FIELDS (for migration/compatibility)
    @Column(name = "encrypted_aadhar_number", length = 512)
    private String encryptedAadharNumber;

    @Column(name = "encrypted_pan_card_number", length = 512)
    private String encryptedPanCardNumber;

    @Column(name = "encrypted_phone", length = 512)
    private String encryptedPhone;

    @Column(name = "encrypted_salary", length = 512)
    private String encryptedSalary;

    @Column(name = "encrypted_dob", length = 512)
    private String encryptedDob;

    @Column(name = "encrypted_emergency_contact", length = 512)
    private String encryptedEmergencyContact;

    @Column(name = "encrypted_blood_group", length = 512)
    private String encryptedBloodGroup;

    @Column(name = "encrypted_address", length = 512)
    private String encryptedAddress;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }
}
