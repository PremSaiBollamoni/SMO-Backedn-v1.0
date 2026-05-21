package com.cutm.smo.controller;

import com.cutm.smo.dto.QrAssignmentRequest;
import com.cutm.smo.dto.TrackingRequest;
import com.cutm.smo.dto.MergingRequest;
import com.cutm.smo.services.SupervisorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supervisor")
@CrossOrigin(origins = "*")
public class SupervisorController {

    @Autowired
    private SupervisorService supervisorService;

    /**
     * Get all process plan numbers (routing IDs with APPROVED status)
     */
    @GetMapping("/process-plans")
    public ResponseEntity<List<String>> getProcessPlans() {
        List<String> processPlans = supervisorService.getProcessPlans();
        return ResponseEntity.ok(processPlans);
    }

    /**
     * Get all styles
     */
    @GetMapping("/styles")
    public ResponseEntity<List<String>> getStyles() {
        List<String> styles = supervisorService.getStyles();
        return ResponseEntity.ok(styles);
    }

    /**
     * Get all sizes from style_variant table
     */
    @GetMapping("/sizes")
    public ResponseEntity<List<String>> getSizes() {
        List<String> sizes = supervisorService.getSizes();
        return ResponseEntity.ok(sizes);
    }

    /**
     * Get all GTG numbers from style_variant table
     */
    @GetMapping("/gtg-numbers")
    public ResponseEntity<List<String>> getGtgNumbers() {
        List<String> gtgNumbers = supervisorService.getGtgNumbers();
        return ResponseEntity.ok(gtgNumbers);
    }

    /**
     * Get all button numbers from buttons table
     */
    @GetMapping("/btn-numbers")
    public ResponseEntity<List<String>> getBtnNumbers() {
        List<String> btnNumbers = supervisorService.getBtnNumbers();
        return ResponseEntity.ok(btnNumbers);
    }

    /**
     * Get all labels from style table (main_label and branding_label)
     */
    @GetMapping("/labels")
    public ResponseEntity<List<String>> getLabels() {
        List<String> labels = supervisorService.getLabels();
        return ResponseEntity.ok(labels);
    }

    /**
     * Get operations for a specific routing/process plan
     */
    @GetMapping("/operations/{routingId}")
    public ResponseEntity<List<Map<String, Object>>> getOperationsForRouting(@PathVariable Long routingId) {
        List<Map<String, Object>> operations = supervisorService.getOperationsForRouting(routingId);
        return ResponseEntity.ok(operations);
    }

    /**
     * Get bin current operation by tray QR code
     */
    @GetMapping("/bin-current-operation/{trayQr}")
    public ResponseEntity<Map<String, Object>> getBinCurrentOperation(@PathVariable String trayQr) {
        Map<String, Object> binInfo = supervisorService.getBinCurrentOperation(trayQr);
        return ResponseEntity.ok(binInfo);
    }

    /**
     * Submit QR assignment
     */
    @PostMapping("/qr-assignment")
    public ResponseEntity<Map<String, Object>> submitQrAssignment(@RequestBody QrAssignmentRequest request) {
        Map<String, Object> response = supervisorService.submitQrAssignment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit tracking data
     */
    @PostMapping("/tracking")
    public ResponseEntity<Map<String, Object>> submitTracking(@RequestBody TrackingRequest request) {
        Map<String, Object> response = supervisorService.submitTracking(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit merging data
     */
    @PostMapping("/merging")
    public ResponseEntity<Map<String, Object>> submitMerging(@RequestBody MergingRequest request) {
        Map<String, Object> response = supervisorService.submitMerging(request);
        return ResponseEntity.ok(response);
    }
}
