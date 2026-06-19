package com.cutm.smo.controller;

import com.cutm.smo.models.Workstation;
import com.cutm.smo.services.WorkstationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/workstations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkstationController {

    private final WorkstationService workstationService;

    @GetMapping
    public List<Workstation> getAll() {
        return workstationService.getAll();
    }

    @PostMapping
    public Workstation create(@RequestBody Map<String, String> body) {
        return workstationService.create(body.get("wsCode"), body.get("machineCode"));
    }

    @PatchMapping("/{wsId}/assign-operation")
    public Workstation assignOperation(@PathVariable Long wsId, @RequestBody Map<String, Long> body) {
        return workstationService.assignOperation(wsId, body.get("opId"));
    }

    @PatchMapping("/{wsId}/machine-code")
    public Workstation updateMachineCode(@PathVariable Long wsId, @RequestBody Map<String, String> body) {
        return workstationService.updateMachineCode(wsId, body.get("machineCode"));
    }

    @PatchMapping("/{wsId}")
    public Workstation update(@PathVariable Long wsId, @RequestBody Map<String, String> body) {
        return workstationService.update(wsId, body.get("wsCode"), body.get("machineCode"));
    }

    @DeleteMapping("/{wsId}")
    public void delete(@PathVariable Long wsId) {
        workstationService.delete(wsId);
    }
}
