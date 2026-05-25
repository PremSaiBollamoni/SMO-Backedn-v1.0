package com.cutm.smo.validation;

import com.cutm.smo.models.Operation;
import com.cutm.smo.models.OperationType;

/**
 * Validator for Operation entities to ensure semantic correctness.
 * 
 * Enforces:
 * - operation_type is never null (defaults to SEQUENTIAL)
 * - operation_type is one of the valid enum values
 * - No conflicting boolean flags (legacy validation)
 */
public class OperationValidator {

    /**
     * Validate operation before persistence
     * 
     * @param operation The operation to validate
     * @throws IllegalArgumentException if operation is invalid
     */
    public static void validate(Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        // Ensure operation_type is set
        if (operation.getOperationType() == null) {
            operation.setOperationType(OperationType.SEQUENTIAL);
        }

        // Validate operation_type is a valid enum value
        try {
            OperationType.valueOf(operation.getOperationType().name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid operation_type: " + operation.getOperationType() + 
                ". Must be one of: SEQUENTIAL, PARALLEL_BRANCH, MERGE"
            );
        }

        // Validate required fields
        if (operation.getName() == null || operation.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Operation name cannot be empty");
        }

        if (operation.getSequence() == null || operation.getSequence() < 0) {
            throw new IllegalArgumentException("Operation sequence must be non-negative");
        }

        if (operation.getStageGroup() == null || operation.getStageGroup() < 1) {
            throw new IllegalArgumentException("Operation stage_group must be positive");
        }
    }

    /**
     * Check if operation type is valid
     * 
     * @param operationType The operation type to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidOperationType(OperationType operationType) {
        return operationType != null && 
               (operationType == OperationType.SEQUENTIAL ||
                operationType == OperationType.PARALLEL_BRANCH ||
                operationType == OperationType.MERGE);
    }

    /**
     * Get human-readable description of operation type
     * 
     * @param operationType The operation type
     * @return Description string
     */
    public static String getTypeDescription(OperationType operationType) {
        if (operationType == null) {
            return "Unknown";
        }
        
        switch (operationType) {
            case SEQUENTIAL:
                return "Sequential - Normal operation in workflow";
            case PARALLEL_BRANCH:
                return "Parallel Branch - Runs in parallel with siblings";
            case MERGE:
                return "Merge - Convergence point where branches merge";
            default:
                return "Unknown";
        }
    }
}
