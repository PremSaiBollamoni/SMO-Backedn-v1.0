package com.cutm.smo.controller;

import com.cutm.smo.models.Operation;
import com.cutm.smo.repositories.OperationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hr/operations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Operations", description = "Manufacturing operations (34 total: 27 Sub Assembly + 7 Full Garment)")
public class OperationController {

    private final OperationRepository operationRepository;

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "List all active operations", description = "Get all ACTIVE operations sorted by sequence number")
    @ApiResponse(responseCode = "200", description = "Operations list retrieved successfully")
    public List<Operation> getActive() {
        return operationRepository.findByStatusOrderBySequenceNoAsc("ACTIVE");
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Create new operation", description = "Create a new manufacturing operation with SAM and skill grade")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation created successfully"),
        @ApiResponse(responseCode = "409", description = "Operation code already exists"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public Operation create(@org.springframework.web.bind.annotation.RequestBody OperationRequest req) {
        if (operationRepository.findByOpCode(req.getOpCode()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Operation code already exists: " + req.getOpCode());
        Operation op = new Operation();
        op.setOpCode(req.getOpCode());
        op.setOpName(req.getOpName());
        op.setStage(req.getStage());
        op.setSequenceNo(req.getSequenceNo());
        op.setSam(req.getSam());
        op.setSkillGrade(req.getSkillGrade());
        op.setTargetPcs(req.getTargetPcs());
        op.setStatus("ACTIVE");
        return operationRepository.save(op);
    }

    @PatchMapping("/{opId}/sam")
    @io.swagger.v3.oas.annotations.Operation(summary = "Update operation details", description = "Update operation: name, stage, SAM, skill grade, target pieces, sequence")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation updated successfully"),
        @ApiResponse(responseCode = "404", description = "Operation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public Operation update(@PathVariable Long opId, @org.springframework.web.bind.annotation.RequestBody OperationRequest req) {
        Operation op = operationRepository.findById(opId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
        if (req.getOpName() != null) op.setOpName(req.getOpName());
        if (req.getStage() != null) op.setStage(req.getStage());
        if (req.getSequenceNo() != null) op.setSequenceNo(req.getSequenceNo());
        if (req.getSam() != null) op.setSam(req.getSam());
        if (req.getSkillGrade() != null) op.setSkillGrade(req.getSkillGrade());
        if (req.getTargetPcs() != null) op.setTargetPcs(req.getTargetPcs());
        return operationRepository.save(op);
    }

    @DeleteMapping("/{opId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Deactivate operation", description = "Mark an operation as INACTIVE")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    public void deactivate(@PathVariable Long opId) {
        Operation op = operationRepository.findById(opId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
        op.setStatus("INACTIVE");
        operationRepository.save(op);
    }

    @Data
    static class OperationRequest {
        private String opCode;
        private String opName;
        private String stage;
        private Integer sequenceNo;
        private BigDecimal sam;
        private String skillGrade;
        private Integer targetPcs;
    }
}
