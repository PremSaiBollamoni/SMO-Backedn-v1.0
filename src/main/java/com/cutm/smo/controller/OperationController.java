package com.cutm.smo.controller;

import com.cutm.smo.models.Operation;
import com.cutm.smo.repositories.OperationRepository;
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
public class OperationController {

    private final OperationRepository operationRepository;

    @GetMapping
    public List<Operation> getActive() {
        return operationRepository.findByStatusOrderBySequenceNoAsc("ACTIVE");
    }

    @PostMapping
    public Operation create(@RequestBody OperationRequest req) {
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
    public Operation update(@PathVariable Long opId, @RequestBody OperationRequest req) {
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
