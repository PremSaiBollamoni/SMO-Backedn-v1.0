package com.cutm.smo.services;

import com.cutm.smo.dto.*;
import com.cutm.smo.models.ShiftBreak;
import com.cutm.smo.models.ShiftTemplate;
import com.cutm.smo.repositories.ShiftBreakRepository;
import com.cutm.smo.repositories.ShiftTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftTemplateRepository shiftTemplateRepo;
    private final ShiftBreakRepository shiftBreakRepo;

    public List<ShiftTemplateDto> getAllShifts() {
        return shiftTemplateRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ShiftTemplateDto> getActiveShifts() {
        return shiftTemplateRepo.findByStatusOrderByStartTimeAsc("ACTIVE").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShiftTemplateDto createShift(CreateShiftTemplateRequest req, Long createdBy) {
        ShiftTemplate t = new ShiftTemplate();
        t.setShiftName(req.getShiftName());
        t.setStartTime(req.getStartTime());
        t.setEndTime(req.getEndTime());
        t.setStatus("ACTIVE");
        t.setCreatedBy(createdBy);
        return toDto(shiftTemplateRepo.save(t));
    }

    @Transactional
    public ShiftTemplateDto addBreak(Long templateId, AddBreakRequest req) {
        ShiftTemplate template = shiftTemplateRepo.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));
        ShiftBreak b = new ShiftBreak();
        b.setShiftTemplate(template);
        b.setBreakName(req.getBreakName());
        b.setStartTime(req.getStartTime());
        b.setEndTime(req.getEndTime());
        shiftBreakRepo.save(b);
        return toDto(shiftTemplateRepo.findById(templateId).get());
    }

    @Transactional
    public void deleteBreak(Long breakId) {
        if (!shiftBreakRepo.existsById(breakId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Break not found");
        }
        shiftBreakRepo.deleteById(breakId);
    }

    @Transactional
    public ShiftTemplateDto toggleStatus(Long templateId) {
        ShiftTemplate t = shiftTemplateRepo.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));
        t.setStatus("ACTIVE".equals(t.getStatus()) ? "INACTIVE" : "ACTIVE");
        return toDto(shiftTemplateRepo.save(t));
    }

    @Transactional
    public void deleteShift(Long templateId) {
        if (!shiftTemplateRepo.existsById(templateId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found");
        }
        shiftTemplateRepo.deleteById(templateId);
    }

    private ShiftTemplateDto toDto(ShiftTemplate t) {
        ShiftTemplateDto dto = new ShiftTemplateDto();
        dto.setTemplateId(t.getTemplateId());
        dto.setShiftName(t.getShiftName());
        dto.setStartTime(t.getStartTime());
        dto.setEndTime(t.getEndTime());
        dto.setStatus(t.getStatus());
        if (t.getBreaks() != null) {
            dto.setBreaks(t.getBreaks().stream().map(b -> {
                ShiftBreakDto bd = new ShiftBreakDto();
                bd.setBreakId(b.getBreakId());
                bd.setBreakName(b.getBreakName());
                bd.setStartTime(b.getStartTime());
                bd.setEndTime(b.getEndTime());
                return bd;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
