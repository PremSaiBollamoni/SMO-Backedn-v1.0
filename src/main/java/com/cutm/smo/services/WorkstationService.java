package com.cutm.smo.services;

import com.cutm.smo.models.Operation;
import com.cutm.smo.models.Workstation;
import com.cutm.smo.repositories.OperationRepository;
import com.cutm.smo.repositories.WorkstationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkstationService {

    private final WorkstationRepository wsRepo;
    private final OperationRepository opRepo;

    public List<Workstation> getAll() {
        return wsRepo.findAllActiveOrderByOperationSequence();
    }

    public Workstation create(String wsCode, String machineCode) {
        if (wsCode == null || wsCode.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wsCode is required");
        Workstation ws = new Workstation();
        ws.setWsCode(wsCode.trim().toUpperCase());
        ws.setMachineCode(machineCode != null ? machineCode.trim().toUpperCase() : null);
        return wsRepo.save(ws);
    }

    public Workstation assignOperation(Long wsId, Long opId) {
        Workstation ws = wsRepo.findById(wsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workstation not found"));
        Operation op = opRepo.findById(opId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
        ws.setOperation(op);
        return wsRepo.save(ws);
    }

    public Workstation updateMachineCode(Long wsId, String machineCode) {
        Workstation ws = wsRepo.findById(wsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workstation not found"));
        ws.setMachineCode(machineCode != null ? machineCode.trim().toUpperCase() : null);
        return wsRepo.save(ws);
    }

    public Workstation update(Long wsId, String wsCode, String machineCode) {
        Workstation ws = wsRepo.findById(wsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workstation not found"));
        if (wsCode != null && !wsCode.isBlank()) ws.setWsCode(wsCode.trim());
        if (machineCode != null) ws.setMachineCode(machineCode.isBlank() ? null : machineCode.trim().toUpperCase());
        return wsRepo.save(ws);
    }

    public void delete(Long wsId) {
        wsRepo.deleteById(wsId);
    }
}
