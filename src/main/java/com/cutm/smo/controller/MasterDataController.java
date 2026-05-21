package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.MasterDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gm/masterdata")
@CrossOrigin(origins = "*")
public class MasterDataController {

    @Autowired
    private MasterDataService masterDataService;

    // ==================== STYLES ====================

    @GetMapping("/styles")
    public ResponseEntity<List<Style>> getAllStyles() {
        log.info("GET /api/gm/masterdata/styles - Fetching all styles");
        return ResponseEntity.ok(masterDataService.getAllStyles());
    }

    @GetMapping("/styles/active")
    public ResponseEntity<List<Style>> getActiveStyles() {
        log.info("GET /api/gm/masterdata/styles/active - Fetching active styles");
        return ResponseEntity.ok(masterDataService.getActiveStyles());
    }

    @PostMapping("/styles")
    public ResponseEntity<Map<String, Object>> createStyle(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/styles - Creating style: {}", data);
        return ResponseEntity.ok(masterDataService.createStyle(data));
    }

    @PutMapping("/styles/{id}")
    public ResponseEntity<Map<String, Object>> updateStyle(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/styles/{} - Updating style", id);
        return ResponseEntity.ok(masterDataService.updateStyle(id, data));
    }

    @DeleteMapping("/styles/{id}")
    public ResponseEntity<Map<String, Object>> deleteStyle(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/styles/{} - Deleting style", id);
        return ResponseEntity.ok(masterDataService.deleteStyle(id));
    }

    // ==================== GTG ====================

    @GetMapping("/gtg")
    public ResponseEntity<List<StyleVariant>> getAllGtg() {
        log.info("GET /api/gm/masterdata/gtg - Fetching all GTG");
        return ResponseEntity.ok(masterDataService.getAllGtg());
    }

    @GetMapping("/gtg/active")
    public ResponseEntity<List<StyleVariant>> getActiveGtg() {
        log.info("GET /api/gm/masterdata/gtg/active - Fetching active GTG");
        return ResponseEntity.ok(masterDataService.getActiveGtg());
    }

    @PostMapping("/gtg")
    public ResponseEntity<Map<String, Object>> createGtg(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/gtg - Creating GTG: {}", data);
        return ResponseEntity.ok(masterDataService.createGtg(data));
    }

    @PutMapping("/gtg/{id}")
    public ResponseEntity<Map<String, Object>> updateGtg(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/gtg/{} - Updating GTG", id);
        return ResponseEntity.ok(masterDataService.updateGtg(id, data));
    }

    @DeleteMapping("/gtg/{id}")
    public ResponseEntity<Map<String, Object>> deleteGtg(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/gtg/{} - Deleting GTG", id);
        return ResponseEntity.ok(masterDataService.deleteGtg(id));
    }

    // ==================== BUTTONS ====================

    @GetMapping("/buttons")
    public ResponseEntity<List<Buttons>> getAllButtons() {
        log.info("GET /api/gm/masterdata/buttons - Fetching all buttons");
        return ResponseEntity.ok(masterDataService.getAllButtons());
    }

    @GetMapping("/buttons/active")
    public ResponseEntity<List<Buttons>> getActiveButtons() {
        log.info("GET /api/gm/masterdata/buttons/active - Fetching active buttons");
        return ResponseEntity.ok(masterDataService.getActiveButtons());
    }

    @PostMapping("/buttons")
    public ResponseEntity<Map<String, Object>> createButton(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/buttons - Creating button: {}", data);
        return ResponseEntity.ok(masterDataService.createButton(data));
    }

    @PutMapping("/buttons/{id}")
    public ResponseEntity<Map<String, Object>> updateButton(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/buttons/{} - Updating button", id);
        return ResponseEntity.ok(masterDataService.updateButton(id, data));
    }

    @DeleteMapping("/buttons/{id}")
    public ResponseEntity<Map<String, Object>> deleteButton(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/buttons/{} - Deleting button", id);
        return ResponseEntity.ok(masterDataService.deleteButton(id));
    }

    // ==================== LABELS ====================

    @GetMapping("/labels")
    public ResponseEntity<List<Label>> getAllLabels() {
        log.info("GET /api/gm/masterdata/labels - Fetching all labels");
        return ResponseEntity.ok(masterDataService.getAllLabels());
    }

    @GetMapping("/labels/active")
    public ResponseEntity<List<Label>> getActiveLabels() {
        log.info("GET /api/gm/masterdata/labels/active - Fetching active labels");
        return ResponseEntity.ok(masterDataService.getActiveLabels());
    }

    @PostMapping("/labels")
    public ResponseEntity<Map<String, Object>> createLabel(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/labels - Creating label: {}", data);
        return ResponseEntity.ok(masterDataService.createLabel(data));
    }

    @PutMapping("/labels/{id}")
    public ResponseEntity<Map<String, Object>> updateLabel(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/labels/{} - Updating label", id);
        return ResponseEntity.ok(masterDataService.updateLabel(id, data));
    }

    @DeleteMapping("/labels/{id}")
    public ResponseEntity<Map<String, Object>> deleteLabel(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/labels/{} - Deleting label", id);
        return ResponseEntity.ok(masterDataService.deleteLabel(id));
    }

    // ==================== MACHINES ====================

    @GetMapping("/machines")
    public ResponseEntity<List<Machine>> getAllMachines() {
        log.info("GET /api/gm/masterdata/machines - Fetching all machines");
        return ResponseEntity.ok(masterDataService.getAllMachines());
    }

    @GetMapping("/machines/active")
    public ResponseEntity<List<Machine>> getActiveMachines() {
        log.info("GET /api/gm/masterdata/machines/active - Fetching active machines");
        return ResponseEntity.ok(masterDataService.getActiveMachines());
    }

    @PostMapping("/machines")
    public ResponseEntity<Map<String, Object>> createMachine(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/machines - Creating machine: {}", data);
        return ResponseEntity.ok(masterDataService.createMachine(data));
    }

    @PutMapping("/machines/{id}")
    public ResponseEntity<Map<String, Object>> updateMachine(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/machines/{} - Updating machine", id);
        return ResponseEntity.ok(masterDataService.updateMachine(id, data));
    }

    @DeleteMapping("/machines/{id}")
    public ResponseEntity<Map<String, Object>> deleteMachine(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/machines/{} - Deleting machine", id);
        return ResponseEntity.ok(masterDataService.deleteMachine(id));
    }

    // ==================== THREADS ====================

    @GetMapping("/threads")
    public ResponseEntity<List<Threads>> getAllThreads() {
        log.info("GET /api/gm/masterdata/threads - Fetching all threads");
        return ResponseEntity.ok(masterDataService.getAllThreads());
    }

    @GetMapping("/threads/active")
    public ResponseEntity<List<Threads>> getActiveThreads() {
        log.info("GET /api/gm/masterdata/threads/active - Fetching active threads");
        return ResponseEntity.ok(masterDataService.getActiveThreads());
    }

    @PostMapping("/threads")
    public ResponseEntity<Map<String, Object>> createThread(@RequestBody Map<String, Object> data) {
        log.info("POST /api/gm/masterdata/threads - Creating thread: {}", data);
        return ResponseEntity.ok(masterDataService.createThread(data));
    }

    @PutMapping("/threads/{id}")
    public ResponseEntity<Map<String, Object>> updateThread(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("PUT /api/gm/masterdata/threads/{} - Updating thread", id);
        return ResponseEntity.ok(masterDataService.updateThread(id, data));
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<Map<String, Object>> deleteThread(@PathVariable Long id) {
        log.info("DELETE /api/gm/masterdata/threads/{} - Deleting thread", id);
        return ResponseEntity.ok(masterDataService.deleteThread(id));
    }
}
