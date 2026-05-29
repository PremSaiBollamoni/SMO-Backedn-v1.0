package com.cutm.smo.services;

import java.util.Arrays;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;

import com.cutm.smo.models.EmployeeInfo;
import com.cutm.smo.models.EmployeeLogin;
import com.cutm.smo.models.EmployeeRole;
import com.cutm.smo.models.Role;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.EmployeeLoginRepository;
import com.cutm.smo.repositories.EmployeeRoleRepository;
import com.cutm.smo.repositories.RoleRepository;

@Slf4j
@Service
public class AccessControlService {
    private final EmployeeInfoRepository employeeInfoRepository;
    private final EmployeeLoginRepository employeeLoginRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RoleRepository roleRepository;

    public AccessControlService(
            EmployeeInfoRepository employeeInfoRepository,
            EmployeeLoginRepository employeeLoginRepository,
            EmployeeRoleRepository employeeRoleRepository,
            RoleRepository roleRepository) {
        this.employeeInfoRepository = employeeInfoRepository;
        this.employeeLoginRepository = employeeLoginRepository;
        this.employeeRoleRepository = employeeRoleRepository;
        this.roleRepository = roleRepository;
    }

    public Long require(String actorEmpId, String requiredActivity) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== ACCESS CONTROL CHECK START ===");
            log.info("[AC] Checking access for actorEmpId='{}', requiredActivity='{}'", actorEmpId, requiredActivity);
            
            Long actorId = parseEmpId(actorEmpId);
            log.info("[AC] Parsed actorId: {}", actorId);
            
            EmployeeInfo actor = employeeInfoRepository.findById(actorId)
                    .orElseThrow(() -> {
                        log.error("[AC] Employee not found with ID: {}", actorId);
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor");
                    });
            log.info("[AC] Found employee: {}, status: {}", actor.getEmpId(), actor.getStatus());
            
            EmployeeLogin login = employeeLoginRepository.findById(actorId)
                    .orElseThrow(() -> {
                        log.error("[AC] Login record not found for employee ID: {}", actorId);
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor login");
                    });
            log.info("[AC] Found login record, status: {}", login.getStatus());

            if (!"ACTIVE".equalsIgnoreCase(actor.getStatus()) || !"ACTIVE".equalsIgnoreCase(login.getStatus())) {
                log.warn("[AC] Access denied: User {} is inactive (employee status: {}, login status: {})", 
                    actorId, actor.getStatus(), login.getStatus());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
            }
            
            Role role = actor.getRole();
            log.info("[AC] User role: {}", role != null ? role.getRoleId() : "NULL");
            
            if (role == null || !"ACTIVE".equalsIgnoreCase(role.getStatus())) {
                log.warn("[AC] Access denied: Role for user {} is inactive or null", actorId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is inactive");
            }
            
            String activity = role.getActivity() == null ? "" : role.getActivity();
            log.info("[AC] Role activities: '{}'", activity);
            
            String normalizedRequired = requiredActivity.trim().toUpperCase(Locale.ROOT);
            log.info("[AC] Checking if '{}' contains '{}'", activity, normalizedRequired);
            
            boolean allowed = activity.equalsIgnoreCase("ALL")
                    || activity.equalsIgnoreCase("ADMIN")
                    || Arrays.stream(activity.split(","))
                            .map(a -> a.trim().toUpperCase(Locale.ROOT))
                            .anyMatch(a -> a.equals(normalizedRequired));
            
            // If primary role doesn't have the activity, check all assigned roles
            // (supports multi-role employees who switched roles mid-session)
            if (!allowed) {
                java.util.List<EmployeeRole> multiRoles = employeeRoleRepository.findByEmpId(actorId);
                for (EmployeeRole er : multiRoles) {
                    java.util.Optional<Role> extraRole = roleRepository.findById(er.getRoleId());
                    if (extraRole.isPresent() && "ACTIVE".equalsIgnoreCase(extraRole.get().getStatus())) {
                        String extraActivity = extraRole.get().getActivity() == null ? "" : extraRole.get().getActivity();
                        boolean extraAllowed = extraActivity.equalsIgnoreCase("ALL")
                                || extraActivity.equalsIgnoreCase("ADMIN")
                                || Arrays.stream(extraActivity.split(","))
                                        .map(a -> a.trim().toUpperCase(Locale.ROOT))
                                        .anyMatch(a -> a.equals(normalizedRequired));
                        if (extraAllowed) {
                            allowed = true;
                            log.info("[AC] Access GRANTED via multi-role assignment (roleId={})", er.getRoleId());
                            break;
                        }
                    }
                }
            }

            log.info("[AC] Permission check result: allowed={}", allowed);
            
            if (!allowed) {
                log.warn("[AC] Access denied: User {} does not have permission for activity {}", actorId, requiredActivity);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for " + requiredActivity);
            }
            
            log.info("[AC] Access GRANTED for user {} to activity {}", actorId, requiredActivity);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Access Control Check", startTime, endTime);
            log.info("=== ACCESS CONTROL CHECK END - SUCCESS ===");
            return actorId;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("[AC] EXCEPTION: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            LoggingUtil.logError(log, "Access control check failed", e);
            LoggingUtil.logPerformance(log, "Access Control Check (Failed)", startTime, endTime);
            throw e;
        }
    }

    private Long parseEmpId(String value) {
        try {
            log.info("[AC] Parsing Employee ID from value: '{}'", value);
            Long parsed = Long.parseLong(value.trim());
            log.info("[AC] Successfully parsed to: {}", parsed);
            return parsed;
        } catch (Exception ex) {
            log.error("[AC] Failed to parse Employee ID '{}': {}", value, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid actor identity");
        }
    }
}
