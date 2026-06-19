package com.cutm.smo.services;

import com.cutm.smo.dto.CreateRoleRequest;
import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeRole;
import com.cutm.smo.models.Role;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeRoleRepository;
import com.cutm.smo.repositories.RoleRepository;
import com.cutm.smo.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role createRole(CreateRoleRequest request) {
        if (request.getRoleId() == null || request.getRoleId().isBlank()
                || request.getRoleName() == null || request.getRoleName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleId and roleName are required");
        }
        Long roleId = parseRoleIdOrNext(request.getRoleId());
        if (roleRepository.existsById(roleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role id already exists");
        }
        roleRepository.findByRoleNameIgnoreCase(request.getRoleName().trim()).ifPresent(r -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        });
        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleName(request.getRoleName().trim());
        role.setActivity(request.getActivity());
        role.setStatus(normalizeStatus(request.getStatus()));
        return roleRepository.save(role);
    }

    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        List<EmployeeInfo> assigned = employeeInfoRepository.findByRoleRoleId(id);
        if (!assigned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete role: " + assigned.size() + " employee(s) assigned");
        }
        roleRepository.deleteById(id);
    }

    @Transactional
    public void deleteRoles(List<Integer> roleIds) {
        long start = System.currentTimeMillis();
        int ok = 0, fail = 0;
        for (Integer id : roleIds) {
            try {
                Long lid = id.longValue();
                if (!employeeInfoRepository.findByRoleRoleId(lid).isEmpty()) {
                    log.warn("Skipping role {}: employees assigned", id);
                    fail++;
                    continue;
                }
                roleRepository.deleteById(lid);
                ok++;
            } catch (Exception e) {
                log.error("Failed to delete role {}", id, e);
                fail++;
            }
        }
        LoggingUtil.logPerformance(log, "deleteRoles", start, System.currentTimeMillis());
        if (ok == 0 && fail > 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete all roles");
        }
    }

    // ── Multi-role ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> getEmployeeRoles(Long empId) {
        List<EmployeeRole> mappings = employeeRoleRepository.findByEmpId(empId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmployeeRole m : mappings) {
            roleRepository.findById(m.getRoleId()).ifPresent(r -> {
                result.add(Map.of(
                        "roleId", r.getRoleId(),
                        "roleName", r.getRoleName(),
                        "activities", r.getActivity() != null ? r.getActivity() : "",
                        "status", r.getStatus()));
            });
        }
        return result;
    }

    @Transactional
    public void setEmployeeRoles(Long empId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }
        for (Long rid : roleIds) {
            if (!roleRepository.existsById(rid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + rid);
            }
        }
        EmployeeInfo emp = employeeInfoRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employeeRoleRepository.deleteByEmpId(empId);
        for (Long rid : roleIds) {
            EmployeeRole er = new EmployeeRole();
            er.setEmpId(empId);
            er.setRoleId(rid);
            employeeRoleRepository.save(er);
        }
        roleRepository.findById(roleIds.get(0)).ifPresent(primary -> {
            emp.setRole(primary);
            employeeInfoRepository.save(emp);
        });
        log.info("Updated roles for empId={}: {}", empId, roleIds);
    }

    public String getMergedActivities(Long empId) {
        List<EmployeeRole> mappings = employeeRoleRepository.findByEmpId(empId);
        if (mappings.isEmpty()) return null;
        Set<String> all = new LinkedHashSet<>();
        for (EmployeeRole m : mappings) {
            roleRepository.findById(m.getRoleId()).ifPresent(r -> {
                if (r.getActivity() != null && !r.getActivity().isBlank()) {
                    for (String a : r.getActivity().split(",")) {
                        all.add(a.trim());
                    }
                }
            });
        }
        return all.isEmpty() ? null : String.join(",", all);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String normalizeStatus(String status) {
        String s = (status == null || status.isBlank()) ? "ACTIVE" : status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "INACTIVE").contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role status must be ACTIVE or INACTIVE");
        }
        return s;
    }

    private Long parseRoleIdOrNext(String value) {
        if (value == null || value.isBlank()) return roleRepository.findMaxRoleId() + 1;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return roleRepository.findMaxRoleId() + 1;
        }
    }
}
