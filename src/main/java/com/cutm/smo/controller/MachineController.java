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
    public Machine getMachineById(@PathVariable String id) {
        return machineService.getMachineById(id);
    }

    @PostMapping
    public Machine createMachine(@RequestBody Machine machine) {
        return machineService.createMachine(machine);
    }

    @PutMapping("/{id}")
    public Machine updateMachine(@PathVariable String id, @RequestBody Machine machine) {
        return machineService.updateMachine(id, machine);
    }

    @DeleteMapping("/{id}")
    public void deleteMachine(@PathVariable String id) {
        machineService.deleteMachine(id);
    }
}