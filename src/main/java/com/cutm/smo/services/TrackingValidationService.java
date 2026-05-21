package com.cutm.smo.services;

import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.TempActiveAssignment;
import com.cutm.smo.repositories.EmployeeRepository;
import com.cutm.smo.repositories.TempActiveAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Dedicated service for tracking validations
 * Designed to be easily extensible for adding new validation rules
 */
@Service
public class TrackingValidationService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TempActiveAssignmentRepository tempActiveAssignmentRepository;

    /**
     * Run all basic validations required for both assignment and completion flows
     */
    public ValidationResult runAllValidations(TrackingRequest request) {
        // Basic field validation
        ValidationResult basicValidation = validateBasicFields(request);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        // Employee QR parsing and validation
        ValidationResult employeeValidation = validateEmployee(request);
        if (!employeeValidation.isValid()) {
            return employeeValidation;
        }

        // QR existence validation
        ValidationResult qrValidation = validateQRsExist(request, employeeValidation.getEmpId());
        if (!qrValidation.isValid()) {
            return qrValidation;
        }

        // TODO: Add more validation checks here as needed
        // - Machine availability validation
        // - Tray/Bin status validation
        // - Shift timing validation
        // - Employee permissions validation
        // - Workstation capacity validation

        return ValidationResult.success(employeeValidation.getEmpId());
    }

    /**
     * Run assignment-specific validations (only for new assignments)
     */
    public ValidationResult runAssignmentValidations(TrackingRequest request, Long empId) {
        // Check if employee is already assigned to another machine
        ValidationResult employeeAssignmentCheck = validateEmployeeNotAlreadyAssigned(empId);
        if (!employeeAssignmentCheck.isValid()) {
            return employeeAssignmentCheck;
        }

        // Check if machine is already assigned to another employee
        ValidationResult machineAssignmentCheck = validateMachineNotAlreadyAssigned(request.getMachineQr());
        if (!machineAssignmentCheck.isValid()) {
            return machineAssignmentCheck;
        }

        // Check if tray is already in use
        ValidationResult trayAssignmentCheck = validateTrayNotAlreadyAssigned(request.getTrayQr());
        if (!trayAssignmentCheck.isValid()) {
            return trayAssignmentCheck;
        }

        // TODO: Add more assignment-specific validation checks here
        // - Check employee skill level for this machine type
        // - Check if employee is on break/shift
        // - Check machine maintenance status
        // - Check production schedule compatibility
        // - Check workstation capacity limits
        // - Check safety requirements compliance

        return ValidationResult.success(empId);
    }

    /**
     * Validate basic required fields
     */
    private ValidationResult validateBasicFields(TrackingRequest request) {
        if (request.getMachineQr() == null || request.getMachineQr().trim().isEmpty()) {
            return ValidationResult.error("Machine QR is required");
        }

        if (request.getEmployeeQr() == null || request.getEmployeeQr().trim().isEmpty()) {
            return ValidationResult.error("Employee QR is required");
        }

        if (request.getTrayQr() == null || request.getTrayQr().trim().isEmpty()) {
            return ValidationResult.error("Tray QR is required");
        }

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            return ValidationResult.error("Status is required");
        }

        return ValidationResult.success(null);
    }

    /**
     * Validate employee QR and parse employee ID
     */
    private ValidationResult validateEmployee(TrackingRequest request) {
        Long empId = parseEmployeeId(request.getEmployeeQr());
        if (empId == null) {
            return ValidationResult.error("Invalid Employee QR format");
        }

        Optional<EmployeeInfo> employee = employeeRepository.findById(empId);
        if (!employee.isPresent()) {
            return ValidationResult.error("Invalid Employee QR - Employee not found");
        }

        // TODO: Add more employee-specific validations
        // - Check if employee is active
        // - Check if employee is on current shift
        // - Check employee permissions for this operation
        // - Check if employee has required certifications
        // - Check employee attendance status

        return ValidationResult.success(empId);
    }

    /**
     * Validate that QRs exist in the system
     */
    private ValidationResult validateQRsExist(TrackingRequest request, Long empId) {
        // Employee validation already done in validateEmployee()

        // TODO: Add machine QR validation
        // - Check if machine exists in machine table
        // - Check if machine is operational
        // - Check if machine is not under maintenance
        // - Check machine type compatibility

        // TODO: Add tray QR validation
        // - Check if tray/bin exists in bin table
        // - Check if tray is not damaged or out of service
        // - Check tray capacity and current load
        // - Check tray type compatibility with machine

        return ValidationResult.success(empId);
    }

    /**
     * Check if employee is already assigned to another machine
     */
    private ValidationResult validateEmployeeNotAlreadyAssigned(Long empId) {
        // Check if employee has any active assignments
        // This prevents double-booking of employees
        
        Optional<TempActiveAssignment> existingAssignment = tempActiveAssignmentRepository
            .findByEmpIdAndStatus(empId, "assigned");
        if (existingAssignment.isPresent()) {
            return ValidationResult.error("Employee is already assigned to another machine");
        }

        return ValidationResult.success(empId);
    }

    /**
     * Check if machine is already assigned to another employee
     */
    private ValidationResult validateMachineNotAlreadyAssigned(String machineQr) {
        // Check if machine has any active assignments
        // This prevents double-booking of machines
        
        Optional<TempActiveAssignment> existingAssignment = tempActiveAssignmentRepository
            .findByMachineQrAndStatus(machineQr, "assigned");
        if (existingAssignment.isPresent()) {
            return ValidationResult.error("Machine is already assigned to another employee");
        }

        return ValidationResult.success(null);
    }

    /**
     * Check if tray is already in use
     */
    private ValidationResult validateTrayNotAlreadyAssigned(String trayQr) {
        // Check if tray has any active assignments
        // This prevents double-booking of trays
        
        Optional<TempActiveAssignment> existingAssignment = tempActiveAssignmentRepository
            .findByTrayQrAndStatus(trayQr, "assigned");
        if (existingAssignment.isPresent()) {
            return ValidationResult.error("Tray is already assigned to another operation");
        }

        return ValidationResult.success(null);
    }

    /**
     * Parse employee ID from employee QR code
     */
    private Long parseEmployeeId(String employeeQr) {
        try {
            // Assuming QR code contains just the employee ID
            // Adjust this logic based on your actual QR code format
            return Long.parseLong(employeeQr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Inner class to hold validation results
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Long empId;

        private ValidationResult(boolean valid, String errorMessage, Long empId) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.empId = empId;
        }

        public static ValidationResult success(Long empId) {
            return new ValidationResult(true, null, empId);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Long getEmpId() {
            return empId;
        }
    }
}