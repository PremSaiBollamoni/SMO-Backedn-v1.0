package com.cutm.smo.services;

import com.cutm.smo.dto.*;
import com.cutm.smo.models.*;
import com.cutm.smo.repositories.EmployeeInfoRepository;
import com.cutm.smo.repositories.JobAssignmentRepository;
import com.cutm.smo.repositories.WorkstationRepository;
import com.cutm.smo.repositories.TrayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobAssignmentRepository jobRepo;
    private final WorkstationRepository wsRepo;
    private final EmployeeInfoRepository empRepo;
    private final TrayRepository trayRepo;

    @Transactional
    public ScanBundleResponse scanTray(String trayNumber, Long wsId, Long empId, Long assignedBy, Integer trayQty) {
        Tray tray = trayRepo.findByTrayNumber(trayNumber).orElseGet(() -> {
            Tray newTray = new Tray();
            newTray.setTrayNumber(trayNumber);
            newTray.setStatus("FREE");
            newTray.setCreatedAt(LocalDateTime.now());
            return trayRepo.save(newTray);
        });

        // If tray is ASSIGNED to this employee at this station, unassign it (complete job)
        var existing = jobRepo.findActiveByStationAndEmp(wsId, empId);
        if (existing.isPresent() && existing.get().getTray().getTrayId().equals(tray.getTrayId())) {
            tray.setStatus("FREE");
            tray.setAssignedTo(null);
            tray.setUnassignedAt(LocalDateTime.now());
            tray.setUnassignedBy(assignedBy);
            trayRepo.save(tray);
            return completeJob(existing.get());
        }

        // If tray is FREE, assign it to employee and create job
        if ("FREE".equals(tray.getStatus())) {
            Workstation ws = wsRepo.findById(wsId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workstation not found"));
            if (ws.getOperation() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station has no operation assigned");

            int qty = trayQty != null && trayQty > 0 ? trayQty : 50;

            Operation op = ws.getOperation();
            BigDecimal sam = op.getSam();
            if (sam == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No SAM value for operation: " + op.getOpName());

            BigDecimal estMinutes = sam.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            // Assign tray
            tray.setStatus("ASSIGNED");
            tray.setAssignedTo(empId);
            tray.setAssignedBy(assignedBy);
            tray.setAssignedAt(LocalDateTime.now());
            trayRepo.save(tray);

            // Create job
            JobAssignment job = new JobAssignment();
            job.setEmpId(empId);
            job.setWorkstation(ws);
            job.setTray(tray);
            job.setOperation(op);
            job.setBundleQty(qty);
            job.setSamValue(sam);
            job.setEstMinutes(estMinutes);
            job.setAssignedBy(assignedBy);
            job.setStatus("IN_PROGRESS");
            jobRepo.save(job);

            ScanBundleResponse res = new ScanBundleResponse();
            res.setAction("ASSIGN");
            res.setJobId(job.getJobId());
            res.setBarcode(trayNumber);
            res.setWsCode(ws.getWsCode());
            res.setOpName(op.getOpName());
            res.setSkillGrade(op.getSkillGrade());
            res.setBundleQty(qty);
            res.setSamValue(sam);
            res.setEstMinutes(estMinutes);
            return res;
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Tray is already assigned to another employee");
    }

    @Deprecated
    @Transactional
    public ScanBundleResponse scanBundle(String barcode, Long wsId, Long empId, Long assignedBy, Integer bundleQty) {
        return scanTray(barcode, wsId, empId, assignedBy, bundleQty);
    }

    @Transactional
    public ScanBundleResponse completeJob(JobAssignment job) {
        LocalDateTime now = LocalDateTime.now();
        long mins = ChronoUnit.SECONDS.between(job.getStartTime(), now);
        BigDecimal actualMinutes = BigDecimal.valueOf(mins).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal samEarned = job.getSamValue().multiply(BigDecimal.valueOf(job.getBundleQty()));
        BigDecimal efficiency = actualMinutes.compareTo(BigDecimal.ZERO) > 0
                ? samEarned.divide(actualMinutes, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        job.setEndTime(now);
        job.setActualMinutes(actualMinutes);
        job.setEfficiencyPct(efficiency);
        job.setStatus("COMPLETED");
        jobRepo.save(job);

        JobCompleteDto dto = new JobCompleteDto();
        dto.setJobId(job.getJobId());
        dto.setOpName(job.getOperation().getOpName());
        dto.setBundleQty(job.getBundleQty());
        dto.setSamValue(job.getSamValue());
        dto.setEstMinutes(job.getEstMinutes());
        dto.setActualMinutes(actualMinutes);
        dto.setEfficiencyPct(efficiency);
        dto.setStartTime(job.getStartTime());
        dto.setEndTime(now);
        empRepo.findById(job.getEmpId()).ifPresent(e -> dto.setEmpName(e.getEmpName()));

        ScanBundleResponse res = new ScanBundleResponse();
        res.setAction("COMPLETE");
        res.setJobId(job.getJobId());
        res.setBarcode(job.getTray().getTrayNumber());
        res.setCompletedJob(dto);
        return res;
    }

    public List<ActiveJobDto> getActiveJobsByStation(Long wsId) {
        return jobRepo.findAllActiveByStation(wsId).stream().map(j -> {
            ActiveJobDto dto = new ActiveJobDto();
            dto.setJobId(j.getJobId());
            dto.setEmpId(j.getEmpId());
            dto.setWsId(j.getWorkstation().getWsId());
            dto.setWsCode(j.getWorkstation().getWsCode());
            dto.setOpName(j.getOperation().getOpName());
            dto.setSkillGrade(j.getOperation().getSkillGrade());
            dto.setBarcode(j.getTray() != null ? j.getTray().getTrayNumber() : "UNKNOWN");
            dto.setBundleQty(j.getBundleQty());
            dto.setSamValue(j.getSamValue());
            dto.setEstMinutes(j.getEstMinutes());
            dto.setStartTime(j.getStartTime());
            dto.setElapsedSeconds(ChronoUnit.SECONDS.between(j.getStartTime(), LocalDateTime.now()));
            dto.setStatus(j.getStatus());
            dto.setTargetPcs(j.getOperation().getTargetPcs());
            empRepo.findById(j.getEmpId()).ifPresent(e -> dto.setEmpName(e.getEmpName()));
            return dto;
        }).toList();
    }

    public List<ActiveJobDto> getActiveJobs() {
        return jobRepo.findByStatusOrderByStartTimeAsc("IN_PROGRESS").stream().map(j -> {
            ActiveJobDto dto = new ActiveJobDto();
            dto.setJobId(j.getJobId());
            dto.setEmpId(j.getEmpId());
            dto.setWsId(j.getWorkstation().getWsId());
            dto.setWsCode(j.getWorkstation().getWsCode());
            dto.setOpName(j.getOperation().getOpName());
            dto.setSkillGrade(j.getOperation().getSkillGrade());
            dto.setBarcode(j.getTray() != null ? j.getTray().getTrayNumber() : "UNKNOWN");
            dto.setBundleQty(j.getBundleQty());
            dto.setSamValue(j.getSamValue());
            dto.setEstMinutes(j.getEstMinutes());
            dto.setStartTime(j.getStartTime());
            dto.setElapsedSeconds(ChronoUnit.SECONDS.between(j.getStartTime(), LocalDateTime.now()));
            dto.setStatus(j.getStatus());
            dto.setTargetPcs(j.getOperation().getTargetPcs());
            empRepo.findById(j.getEmpId()).ifPresent(e -> dto.setEmpName(e.getEmpName()));
            return dto;
        }).toList();
    }

    public List<ActiveJobDto> getAllJobs() {
        return jobRepo.findAll().stream().map(j -> {
            ActiveJobDto dto = new ActiveJobDto();
            dto.setJobId(j.getJobId());
            dto.setEmpId(j.getEmpId());
            dto.setWsId(j.getWorkstation().getWsId());
            dto.setWsCode(j.getWorkstation().getWsCode());
            dto.setOpName(j.getOperation().getOpName());
            dto.setSkillGrade(j.getOperation().getSkillGrade());
            dto.setBarcode(j.getTray() != null ? j.getTray().getTrayNumber() : "UNKNOWN");
            dto.setBundleQty(j.getBundleQty());
            dto.setSamValue(j.getSamValue());
            dto.setEstMinutes(j.getEstMinutes());
            dto.setStartTime(j.getStartTime());
            dto.setEndTime(j.getEndTime());
            dto.setElapsedSeconds(j.getEndTime() != null
                ? ChronoUnit.SECONDS.between(j.getStartTime(), j.getEndTime())
                : ChronoUnit.SECONDS.between(j.getStartTime(), LocalDateTime.now()));
            dto.setStatus(j.getStatus());
            dto.setEfficiencyPct(j.getEfficiencyPct());
            dto.setTargetPcs(j.getOperation().getTargetPcs());
            empRepo.findById(j.getEmpId()).ifPresent(e -> dto.setEmpName(e.getEmpName()));
            return dto;
        }).toList();
    }
}
