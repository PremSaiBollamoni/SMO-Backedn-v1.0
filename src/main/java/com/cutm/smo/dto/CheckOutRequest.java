package com.cutm.smo.dto;

import lombok.Data;

@Data
public class CheckOutRequest {
    private String tempQrToken;
    private Long markedBy;
}
