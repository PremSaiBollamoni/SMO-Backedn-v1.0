package com.cutm.smo.services;

import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<EmployeeLogin> getLoginById(Long empId) {
        return employeeLoginRepository.findById(empId);
    }

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

        // ENFORCE BCRYPT PASSWORD HASHING
        String plainPassword = login.getPassword();
        String hashedPassword = passwordEncoder.encode(plainPassword);
        login.setPassword(hashedPassword);
        login.setPasswordHashVersion(2);  // 2 = BCrypt
        login.setPasswordChangedAt(LocalDateTime.now());
        login.setFailedLoginAttempts(0);
        login.setLocked(false);

        log.info("Creating login for employee {} with BCrypt-hashed password", login.getEmpId());
        return employeeLoginRepository.save(login);
    }

    public EmployeeLogin updateLogin(Long empId, EmployeeLogin patch) {
        if (!employeeInfoRepository.existsById(empId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        EmployeeLogin existing = employeeLoginRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        if (patch != null && patch.getPassword() != null && !patch.getPassword().isBlank()) {
            // ENFORCE BCRYPT PASSWORD HASHING
            String plainPassword = patch.getPassword().trim();
            String hashedPassword = passwordEncoder.encode(plainPassword);
            existing.setPassword(hashedPassword);
            existing.setPasswordHashVersion(2);  // 2 = BCrypt
            existing.setPasswordChangedAt(LocalDateTime.now());
            existing.setFailedLoginAttempts(0);
            log.info("Password updated for employee {} with BCrypt hashing", empId);
        }
        if (patch != null && patch.getStatus() != null && !patch.getStatus().isBlank())
            existing.setStatus(patch.getStatus().trim().toUpperCase(Locale.ROOT));
        return employeeLoginRepository.save(existing);
    }

    public void updatePassword(Long empId, String newPassword) {
        employeeLoginRepository.findById(empId).ifPresent(login -> {
            // ENFORCE BCRYPT PASSWORD HASHING
            String hashedPassword = passwordEncoder.encode(newPassword.trim());
            login.setPassword(hashedPassword);
            login.setPasswordHashVersion(2);  // 2 = BCrypt
            login.setPasswordChangedAt(LocalDateTime.now());
            login.setFailedLoginAttempts(0);
            login.setLocked(false);
            employeeLoginRepository.save(login);
            log.info("Password updated for employee {} with BCrypt hashing", empId);
        });
    }
}
