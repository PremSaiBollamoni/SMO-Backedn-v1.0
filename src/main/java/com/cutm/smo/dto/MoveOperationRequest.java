package com.cutm.smo.dto;

import lombok.Data;

/**
 * Request body for moving an existing operation to a new position in a routing.
 *
 * Strategy:
 *   1) Detach the operation from its current spot (auto-bridges its
 *      predecessors → successors so the flow stays connected).
 *   2) Re-insert it at the new spot using the same logic as insert (split-edge
 *      OR add-branch). The 'mode' field decides which path is used.
 *
 * For SPLIT_EDGE/AFTER:  new anchor's outgoing edges get split — same rules as InsertOperationRequest.
 * For SPLIT_EDGE/BEFORE: new anchor's incoming edges get split.
 * For ADD_BRANCH: a new outgoing edge is added from anchor; optional mergeTargetOperationId
 *                 connects it back into the graph.
 *
 * If position == "TERMINAL", the operation is just detached (becomes a terminal node
 * with NO incoming edges — useful for "make this a terminal node").
 */
@Data
public class MoveOperationRequest {
    /** The operation being moved (must be in this routing). */
    private Long operationId;

    /** "SPLIT_EDGE", "ADD_BRANCH", or "TERMINAL". Defaults to SPLIT_EDGE. */
    private String mode;

    /** "AFTER" or "BEFORE" (required for SPLIT_EDGE). Ignored for TERMINAL. */
    private String position;

    /** New anchor for AFTER/BEFORE/ADD_BRANCH (required, except for TERMINAL). */
    private Long anchorOperationId;

    /** When SPLIT_EDGE has multiple branches, this picks the specific edge. */
    private Long otherEndOperationId;

    /** Used in ADD_BRANCH mode only. */
    private Long mergeTargetOperationId;

    /**
     * If true, when detaching the operation, do NOT auto-bridge its
     * predecessors → successors. Defaults to false (auto-bridge enabled).
     */
    private boolean skipAutoBridge;
}
