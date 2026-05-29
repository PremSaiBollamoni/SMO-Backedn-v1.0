package com.cutm.smo.dto;

import lombok.Data;

/**
 * Request body for adding a new connection between two operations that
 * are already part of a routing.
 *
 * fromOperationId -> toOperationId edge is created.
 * Both operations must exist in the routing already.
 * If the same edge already exists, the request is rejected.
 */
@Data
public class AddEdgeRequest {
    private Long fromOperationId;
    private Long toOperationId;
    /** Optional. Defaults to "sequential". */
    private String edgeType;
}
