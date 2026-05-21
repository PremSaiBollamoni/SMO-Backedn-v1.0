package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class MachineService {
    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<Machine> getAllMachines() { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET ALL MACHINES START ===");
            List<Machine> machines = machineRepository.findAll();
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
    
    public Machine getMachineById(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== GET MACHINE BY ID START ===");
            log.debug("Machine ID: {}", id);
            Machine machine = machineRepository.findById(id).orElse(null);
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
    
    public Machine createMachine(Machine machine) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== CREATE MACHINE START ===");
            log.debug("Machine Data: {}", machine);
            Machine saved = machineRepository.save(machine);
            log.info("Machine created successfully with ID: {}", saved.getMachineId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Create Machine", startTime, endTime);
            log.info("=== CREATE MACHINE END - SUCCESS ===");
            return saved;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to create machine", e);
            LoggingUtil.logPerformance(log, "Create Machine (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public Machine updateMachine(Long id, Machine machine) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== UPDATE MACHINE START ===");
            log.debug("Machine ID: {}", id);
            log.debug("Machine Data: {}", machine);
            machine.setMachineId(id);
            Machine updated = machineRepository.save(machine);
            log.info("Machine updated successfully with ID: {}", updated.getMachineId());
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Update Machine", startTime, endTime);
            log.info("=== UPDATE MACHINE END - SUCCESS ===");
            return updated;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to update machine with ID: " + id, e);
            LoggingUtil.logPerformance(log, "Update Machine (Failed)", startTime, endTime);
            throw e;
        }
    }
    
    public void deleteMachine(Long id) { 
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== DELETE MACHINE START ===");
            log.debug("Machine ID to delete: {}", id);
            machineRepository.deleteById(id);
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