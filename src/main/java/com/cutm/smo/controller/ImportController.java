package com.cutm.smo.controller;

import com.cutm.smo.models.Operation;
import com.cutm.smo.models.Workstation;
import com.cutm.smo.repositories.OperationRepository;
import com.cutm.smo.repositories.WorkstationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hr/import")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ImportController {

    private final OperationRepository operationRepo;
    private final WorkstationRepository workstationRepo;

    @PostMapping("/upload")
    public ImportResult uploadExcel(@RequestParam("file") MultipartFile file) throws Exception {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter fmt = new DataFormatter();

        // Scan rows to find the header row (handles title/blank rows before headers)
        Row headerRow = null;
        int headerRowIdx = -1;
        Map<String, Integer> cols = new HashMap<>();

        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            cols.clear();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                if (cell == null) continue;
                String h = fmt.formatCellValue(cell).trim().toLowerCase();
                if (h.equals("stage")) cols.put("stage", i);
                else if (h.contains("station")) cols.put("station", i);
                else if (h.contains("operation name")) cols.put("opName", i);
                else if (h.startsWith("sam")) cols.put("sam", i);
                else if (h.startsWith("target")) cols.put("target", i);
            }
            if (cols.containsKey("opName")) {
                headerRow = row;
                headerRowIdx = r;
                break;
            }
        }

        log.info("Header row found at index: {}, cols: {}", headerRowIdx, cols);

        if (headerRow == null || !cols.containsKey("opName"))
            throw new RuntimeException("Could not find header row with 'Operation Name' in the first 10 rows");

        int opsCreated = 0, opsUpdated = 0, stationsCreated = 0, stationsLinked = 0;
        int seqCounter = 1;

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String opName = cellStr(row, cols.get("opName"));
            if (opName.isBlank()) continue;

            // 1. Find or create operation by name
            Operation op = operationRepo.findByOpNameIgnoreCase(opName).orElse(null);
            boolean isNew = op == null;
            if (isNew) {
                op = new Operation();
                op.setOpCode(generateCode(opName));
                op.setOpName(opName);
                op.setStatus("ACTIVE");
                op.setSequenceNo(seqCounter);
            }

            if (cols.containsKey("stage")) {
                String stage = cellStr(row, cols.get("stage"));
                if (!stage.isBlank()) op.setStage(stage.toUpperCase());
            }
            if (cols.containsKey("sam")) {
                Double sam = cellNum(row, cols.get("sam"));
                if (sam != null) op.setSam(BigDecimal.valueOf(sam));
            }
            if (cols.containsKey("target")) {
                Double target = cellNum(row, cols.get("target"));
                if (target != null) op.setTargetPcs(target.intValue());
            }

            op = operationRepo.save(op);
            if (isNew) opsCreated++; else opsUpdated++;
            seqCounter++;

            // 2. Find or create station and link
            if (cols.containsKey("station")) {
                String wsCode = cellStr(row, cols.get("station"));
                if (!wsCode.isBlank()) {
                    Workstation ws = workstationRepo.findByWsCode(wsCode).orElse(null);
                    if (ws == null) {
                        ws = new Workstation();
                        ws.setWsCode(wsCode);
                        ws.setStatus("ACTIVE");
                        stationsCreated++;
                    }
                    ws.setOperation(op);
                    workstationRepo.save(ws);
                    stationsLinked++;
                }
            }
        }

        workbook.close();
        return new ImportResult(opsCreated, opsUpdated, stationsCreated, stationsLinked);
    }

    private final DataFormatter _fmt = new DataFormatter();

    private String cellStr(Row row, Integer idx) {
        if (idx == null) return "";
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return _fmt.formatCellValue(cell).trim();
    }

    private Double cellNum(Row row, Integer idx) {
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            String s = cell.getStringCellValue().trim();
            return s.equalsIgnoreCase("n/a") || s.isEmpty() ? null : Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateCode(String opName) {
        String[] words = opName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder("OP-");
        for (String w : words) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)));
        String base = sb.toString();
        String code = base;
        int attempt = 1;
        while (operationRepo.findByOpCode(code).isPresent()) code = base + attempt++;
        return code;
    }

    @Data
    public static class ImportResult {
        private final int opsCreated;
        private final int opsUpdated;
        private final int stationsCreated;
        private final int stationsLinked;
    }
}
