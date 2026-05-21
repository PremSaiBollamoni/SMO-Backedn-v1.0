package com.cutm.smo.controller;

import com.cutm.smo.dto.TempQrScanRequest;
import com.cutm.smo.dto.TempQrScanResponse;
import com.cutm.smo.models.QrScanHistory;
import com.cutm.smo.models.TempEmpQr;
import com.cutm.smo.services.TempQrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/temp-qr")
@CrossOrigin(origins = "*")
public class TempQrController {
    
    @Autowired
    private TempQrService tempQrService;
    
    @PostMapping("/scan")
    public ResponseEntity<?> handleQrScan(@RequestBody TempQrScanRequest request) {
        try {
            TempQrScanResponse response = tempQrService.handleQrScan(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process QR scan: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<TempEmpQr>> getActiveMappings() {
        try {
            List<TempEmpQr> mappings = tempQrService.getActiveMappings();
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<TempEmpQr>> getAllMappings() {
        try {
            List<TempEmpQr> mappings = tempQrService.getAllMappings();
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<QrScanHistory>> getScanHistory() {
        try {
            List<QrScanHistory> history = tempQrService.getScanHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/history/{qrId}")
    public ResponseEntity<List<QrScanHistory>> getScanHistoryByQrId(@PathVariable String qrId) {
        try {
            List<QrScanHistory> history = tempQrService.getScanHistoryByQrId(qrId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/employees")
    public ResponseEntity<?> getEmployeesForQrAssignment() {
        try {
            List<?> employees = tempQrService.getAllEmployees();
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch employees: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/unmap/{id}")
    public ResponseEntity<?> unmapQrCode(
        @PathVariable Long id,
        @RequestParam String unmappedBy
    ) {
        try {
            boolean success = tempQrService.unmapQrCode(id, unmappedBy);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "QR code unmapped successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Mapping not found or already completed");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to unmap QR code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
