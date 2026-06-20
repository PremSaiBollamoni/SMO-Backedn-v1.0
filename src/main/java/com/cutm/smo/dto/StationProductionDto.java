package com.cutm.smo.dto;

import lombok.Data;
import java.util.List;

@Data
public class StationProductionDto {
    private Long empId;
    private String empName;
    private String opName;
    private Integer targetPcs;
    private List<SlotDto> slots;
    private Integer totalPieces;
    private Double efficiencyPct;

    @Data
    public static class SlotDto {
        private Integer hourSlot;
        private String timeLabel;
        private Integer piecesProduced;
        private Double slotEfficiencyPct;
    }
}
