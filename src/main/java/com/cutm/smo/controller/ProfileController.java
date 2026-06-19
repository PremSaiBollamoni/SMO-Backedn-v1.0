package com.cutm.smo.controller;

import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.services.EmployeeService;
import com.cutm.smo.services.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hr/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final EmployeeService employeeService;
    private final LoginService loginService;

    /**
     * Get profile - user can view their own profile or HR/Admin can view any profile
     */
    @GetMapping("/{empId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN') || #empId == authentication.principal")
    public EmployeeInfo getProfile(@PathVariable Long empId) {
        // IDOR Protection: Verify the requesting user has permission
        Long currentEmpId = getCurrentEmpId();
        if (!currentEmpId.equals(empId) && !hasHRRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this profile");
        }

        return employeeService.getEmployeeById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    /**
     * Update profile - user can only update their own profile
     */
    @PutMapping("/{empId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN') || #empId == authentication.principal")
    public Map<String, Object> updateProfile(
            @PathVariable Long empId,
            @RequestBody Map<String, Object> body) {

        // IDOR Protection: Verify the requesting user can only update their own profile
        Long currentEmpId = getCurrentEmpId();
        if (!currentEmpId.equals(empId) && !hasHRRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own profile");
        }

        EmployeeInfo emp = employeeService.getEmployeeById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (body.containsKey("empName")) emp.setEmpName((String) body.get("empName"));
        if (body.containsKey("email")) emp.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) emp.setPhone((String) body.get("phone"));
        if (body.containsKey("address")) emp.setAddress((String) body.get("address"));
        if (body.containsKey("dob") && body.get("dob") != null)
            emp.setDob(LocalDate.parse((String) body.get("dob")));
        if (body.containsKey("bloodGroup")) emp.setBloodGroup((String) body.get("bloodGroup"));
        if (body.containsKey("emergencyContact")) emp.setEmergencyContact((String) body.get("emergencyContact"));
        if (body.containsKey("aadharNumber")) emp.setAadharNumber((String) body.get("aadharNumber"));
        if (body.containsKey("panCardNumber")) emp.setPanCardNumber((String) body.get("panCardNumber"));
        // Only HR/ADMIN can change status
        if (body.containsKey("status") && hasHRRole()) emp.setStatus((String) body.get("status"));

        employeeService.saveEmployee(emp);

        if (body.containsKey("password") && body.get("password") != null) {
            String pw = (String) body.get("password");
            if (!pw.trim().isEmpty()) loginService.updatePassword(empId, pw);
        }

        return Map.of("success", true, "message", "Profile updated successfully");
    }

    /**
     * Helper method to get the current authenticated employee ID.
     */
    private Long getCurrentEmpId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        log.warn("Could not determine current user from authentication context");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not determine current user");
    }

    /**
     * Helper method to check if current user has HR role.
     */
    private boolean hasHRRole() {
        String role = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return role != null && (role.equalsIgnoreCase("HR") || role.equalsIgnoreCase("ADMIN"));
    }
}
