package com.cutm.smo.dto;

import lombok.Data;

/**
 * Request body for renaming an operation within a SPECIFIC routing only.
 * If the operation is shared with other routings, the service clones the
 * operation first so the rename does not leak into other routings.
 */
@Data
public class RenameOperationRequest {
    /** The operation currently in the selected routing being renamed */
    private Long operationId;

    /** New name (required, non-empty) */
    private String newName;

    /** New description (optional, can be null/empty to keep existing) */
    private String newDescription;
}
