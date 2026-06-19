package com.cutm.smo.services;

import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeInfoRepository employeeInfoRepository;

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
        return employeeLoginRepository.save(login);
    }

    public EmployeeLogin updateLogin(Long empId, EmployeeLogin patch) {
        if (!employeeInfoRepository.existsById(empId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        EmployeeLogin existing = employeeLoginRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login not found"));
        if (patch != null && patch.getPassword() != null && !patch.getPassword().isBlank())
            existing.setPassword(patch.getPassword().trim());
        if (patch != null && patch.getStatus() != null && !patch.getStatus().isBlank())
            existing.setStatus(patch.getStatus().trim().toUpperCase(Locale.ROOT));
        return employeeLoginRepository.save(existing);
    }

    public void updatePassword(Long empId, String newPassword) {
        employeeLoginRepository.findById(empId).ifPresent(login -> {
            login.setPassword(newPassword.trim());
            employeeLoginRepository.save(login);
        });
    }
}
