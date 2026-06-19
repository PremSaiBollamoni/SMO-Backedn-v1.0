package com.cutm.smo.controller;

import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.services.EmployeeService;
import com.cutm.smo.services.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/profile")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileController {

    private final EmployeeService employeeService;
    private final LoginService loginService;

    @GetMapping("/{empId}")
    public EmployeeInfo getProfile(@PathVariable Long empId) {
        return employeeService.getEmployeeById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PutMapping("/{empId}")
    public Map<String, Object> updateProfile(
            @PathVariable Long empId,
            @RequestBody Map<String, Object> body) {

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
        if (body.containsKey("status")) emp.setStatus((String) body.get("status"));

        employeeService.saveEmployee(emp);

        if (body.containsKey("password") && body.get("password") != null) {
            String pw = (String) body.get("password");
            if (!pw.trim().isEmpty()) loginService.updatePassword(empId, pw);
        }

        return Map.of("success", true, "message", "Profile updated successfully");
    }
}
