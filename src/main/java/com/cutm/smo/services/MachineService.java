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
        return machineRepository.findAll();
    }

    public Machine getMachineById(String id) {
        return machineRepository.findById(id).orElse(null);
    }

    public Machine createMachine(Machine machine) {
        if (machine.getMachineId() == null || machine.getMachineId().trim().isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Machine ID (QR code) is required");
        }
        if (machineRepository.existsById(machine.getMachineId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Machine with ID '" + machine.getMachineId() + "' already exists");
        }
        return machineRepository.save(machine);
    }

    public Machine updateMachine(String id, Machine machine) {
        machine.setMachineId(id);
        return machineRepository.save(machine);
    }

    public void deleteMachine(String id) {
        machineRepository.deleteById(id);
    }
}
