package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/machine")
@CrossOrigin(origins = "*")
public class MachineController {
    private final MachineService machineService;

    public MachineController(MachineService machineService) { this.machineService = machineService; }

    @GetMapping
    public List<Machine> getAllMachines() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MACHINES START ===");
            List<Machine> machines = machineService.getAllMachines();
            log.info("Retrieved {} machines", machines.size());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get All Machines", startTime, endTime);
            log.info("=== GET ALL MACHINES END - SUCCESS ===");
            return machines;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get all machines", e);
            LoggingUtil.logPerformance(log, "Get All Machines (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public Machine getMachineById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET MACHINE BY ID START ===");
            log.debug("Machine ID: {}", id);
            Machine machine = machineService.getMachineById(id);
            if (machine != null) {
                log.info("Machine found: {}", machine.getName());
            } else {
                log.warn("Machine not found with ID: {}", id);
            }
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Get Machine By ID", startTime, endTime);
            log.info("=== GET MACHINE BY ID END - SUCCESS ===");
            return machine;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to get machine with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Get Machine By ID (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PostMapping
    public Machine createMachine(@RequestBody Machine machine) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MACHINE START ===");
            log.debug("Machine Data: {}", machine);
            Machine createdMachine = machineService.createMachine(machine);
            log.info("Machine created successfully with ID: {}", createdMachine.getMachineId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Machine", startTime, endTime);
            log.info("=== CREATE MACHINE END - SUCCESS ===");
            return createdMachine;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create machine", e);
            LoggingUtil.logPerformance(log, "Create Machine (Failed)", startTime, endTime);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public Machine updateMachine(@PathVariable Long id, @RequestBody Machine machine) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE MACHINE START ===");
            log.debug("Machine ID: {}", id);
            log.debug("Machine Data: {}", machine);
            Machine updatedMachine = machineService.updateMachine(id, machine);
            log.info("Machine updated successfully with ID: {}", updatedMachine.getMachineId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Machine", startTime, endTime);
            log.info("=== UPDATE MACHINE END - SUCCESS ===");
            return updatedMachine;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update machine with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Machine (Failed)", startTime, endTime);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteMachine(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE MACHINE START ===");
            log.debug("Machine ID to delete: {}", id);
            machineService.deleteMachine(id);
            log.info("Machine deleted successfully with ID: {}", id);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Delete Machine", startTime, endTime);
            log.info("=== DELETE MACHINE END - SUCCESS ===");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to delete machine with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Delete Machine (Failed)", startTime, endTime);
            throw e;
        }
    }
}