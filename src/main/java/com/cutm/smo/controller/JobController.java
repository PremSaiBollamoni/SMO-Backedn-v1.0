package com.cutm.smo.controller;

import com.cutm.smo.dto.ActiveJobDto;
import com.cutm.smo.dto.AssignJobRequest;
import com.cutm.smo.dto.ScanBundleResponse;
import com.cutm.smo.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/health")
    public String health() {
        return "Job Controller is healthy";
    }

    @PostMapping("/scan")
    public ScanBundleResponse scan(@RequestBody AssignJobRequest req) {
        try {
            log.info("Scan request received - Barcode: {}, WsId: {}, EmpId: {}, AssignedBy: {}",
                    req.getBarcode(), req.getWsId(), req.getEmpId(), req.getAssignedBy());
            ScanBundleResponse response = jobService.scanBundle(req.getBarcode(), req.getWsId(), req.getEmpId(), req.getAssignedBy(), req.getBundleQty());
            log.info("Scan completed successfully");
            return response;
        } catch (Exception e) {
            log.error("Error in scan endpoint: ", e);
            throw e;
        }
    }

    @GetMapping("/active")
    public List<ActiveJobDto> getActiveJobs() {
        return jobService.getActiveJobs();
    }

    @GetMapping("/station/{wsId}/active")
    public List<ActiveJobDto> getActiveJobsByStation(@PathVariable Long wsId) {
        return jobService.getActiveJobsByStation(wsId);
    }
}
