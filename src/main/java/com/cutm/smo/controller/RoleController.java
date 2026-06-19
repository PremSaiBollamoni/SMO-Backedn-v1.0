package com.cutm.smo.controller;

import com.cutm.smo.dto.CreateRoleRequest;
import com.cutm.smo.models.Role;
import com.cutm.smo.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('HR')")
    public List<Role> getAllRoles() {
        log.debug("Fetching all roles");
        return roleService.getAllRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAnyRole('HR')")
    public Role createRole(@RequestBody CreateRoleRequest request) {
        log.info("Creating new role: {}", request.getRoleName());
        return roleService.createRole(request);
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAnyRole('HR')")
    public void deleteRole(@PathVariable Long id) {
        log.info("Deleting role: {}", id);
        roleService.deleteRole(id);
    }

    @DeleteMapping("/roles")
    @PreAuthorize("hasAnyRole('HR')")
    public void deleteRoles(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("roleIds");
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleIds is required");
        }
        log.info("Deleting multiple roles: {}", ids);
        roleService.deleteRoles(ids);
    }

    @GetMapping("/employees/{empId}/roles")
    @PreAuthorize("hasAnyRole('HR')")
    public List<Map<String, Object>> getEmployeeRoles(@PathVariable Long empId) {
        log.debug("Fetching roles for employee: {}", empId);
        return roleService.getEmployeeRoles(empId);
    }

    @PutMapping("/employees/{empId}/roles")
    @PreAuthorize("hasAnyRole('HR')")
    public Map<String, Object> setEmployeeRoles(
            @PathVariable Long empId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleIds is required");
        }
        log.info("Updating roles for employee: {} with roles: {}", empId, roleIds);
        List<Long> ids = roleIds.stream().map(Integer::longValue).toList();
        roleService.setEmployeeRoles(empId, ids);
        return Map.of("success", true, "message", "Roles updated successfully");
    }
}
