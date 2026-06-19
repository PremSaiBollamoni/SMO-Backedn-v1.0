package com.cutm.smo.controller;

import com.cutm.smo.dto.CreateRoleRequest;
import com.cutm.smo.models.Role;
import com.cutm.smo.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PostMapping("/roles")
    public Role createRole(@RequestBody CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @DeleteMapping("/roles/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    @DeleteMapping("/roles")
    public void deleteRoles(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("roleIds");
        if (ids == null || ids.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "roleIds is required");
        roleService.deleteRoles(ids);
    }

    @GetMapping("/employees/{empId}/roles")
    public List<Map<String, Object>> getEmployeeRoles(@PathVariable Long empId) {
        return roleService.getEmployeeRoles(empId);
    }

    @PutMapping("/employees/{empId}/roles")
    public Map<String, Object> setEmployeeRoles(
            @PathVariable Long empId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "roleIds is required");
        List<Long> ids = roleIds.stream().map(Integer::longValue).toList();
        roleService.setEmployeeRoles(empId, ids);
        return Map.of("success", true, "message", "Roles updated successfully");
    }
}
