package com.cutm.smo.dto;

import lombok.Data;

/**
 * Request body for redirecting an existing edge in a routing.
 * Changes the edge fromOperationId -> oldToOperationId  to
 *                  fromOperationId -> newToOperationId.
 *
 * Both old and new targets must be operations that already exist in this routing.
 */
@Data
public class ReconnectEdgeRequest {
    private Long fromOperationId;
    private Long oldToOperationId;
    private Long newToOperationId;
}
