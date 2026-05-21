package com.cutm.smo.models;

/**
 * Enum for mutually exclusive operation types in the manufacturing workflow.
 * 
 * Replaces conflicting boolean flags (is_parallel, merge_point) with a single
 * semantic type that cannot be in an invalid state.
 * 
 * Types:
 * - SEQUENTIAL: Normal sequential operation in the workflow
 * - PARALLEL_BRANCH: Operation that runs in parallel with siblings
 * - MERGE: Convergence point where parallel branches merge back
 */
public enum OperationType {
    SEQUENTIAL("sequential"),
    PARALLEL_BRANCH("parallel_branch"),
    MERGE("merge");

    private final String value;

    OperationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OperationType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SEQUENTIAL; // Default to sequential for null/empty
        }
        
        String normalized = value.trim().toUpperCase();
        
        for (OperationType type : OperationType.values()) {
            if (type.name().equals(normalized) || type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        // Default to sequential if not found
        return SEQUENTIAL;
    }
}
