package com.cutm.smo.dto;

import lombok.Data;

/**
 * Request body for inserting a new operation into a routing.
 *
 * Two modes:
 *   SPLIT_EDGE (default) — existing behavior: insert between two nodes by
 *     splitting an existing edge. (anchor → NEW → otherEnd, removes anchor → otherEnd).
 *   ADD_BRANCH — adds the new op as a NEW parallel branch from the anchor:
 *     creates anchor → NEW (and optionally NEW → mergeTargetOperationId).
 *     No existing edges are removed.
 *
 * For SPLIT_EDGE:
 *   - position="AFTER":  insert between (afterOperationId) -> next op(s)
 *   - position="BEFORE": insert between previous op(s) -> (beforeOperationId)
 *   - When both afterOperationId AND beforeOperationId are provided, splits
 *     that specific edge (use this to choose a branch when multiple exist).
 *
 * For ADD_BRANCH:
 *   - afterOperationId (required) is the anchor — the source of the new branch.
 *   - mergeTargetOperationId (optional) — if set, also create edge NEW → mergeTarget.
 */
@Data
public class InsertOperationRequest {
    /** "SPLIT_EDGE" or "ADD_BRANCH". Defaults to SPLIT_EDGE for backwards compatibility. */
    private String mode;

    /** "AFTER" or "BEFORE" (only used for SPLIT_EDGE) */
    private String position;

    /** Anchor operation - the new op will be inserted right after this one (when position=AFTER) */
    private Long afterOperationId;

    /** Anchor operation - the new op will be inserted right before this one (when position=BEFORE) */
    private Long beforeOperationId;

    /** Used in ADD_BRANCH mode only. If set, an edge NEW -> mergeTargetOperationId is also created. */
    private Long mergeTargetOperationId;

    /** If true, link an existing operation; if false, create a new one */
    private boolean useExisting;

    /** Required when useExisting=true */
    private Long existingOperationId;

    /** Required when useExisting=false */
    private String name;
    private String description;
    private Integer sequence;
    private String operationType;
    private Integer stageGroup;
    private Integer standardTime;
}
