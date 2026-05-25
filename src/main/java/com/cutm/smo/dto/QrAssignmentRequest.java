package com.cutm.smo.dto;

public class QrAssignmentRequest {
    private String processPlanNumber;
    private String qrCode;
    private String style;
    private String size;
    private String gtgNumber;
    private String btnNumber;
    private String label;
    private String nextOperation;
    private Integer trayQuantity;
    private Long supervisorId; // Added for enhanced workflow
    private String notes; // Added for enhanced workflow
    private String orderNumber; // Added for order management - links bin to production order via order number
    private Long operationId; // Added for direct operation assignment

    // Getters and Setters
    public String getProcessPlanNumber() {
        return processPlanNumber;
    }

    public void setProcessPlanNumber(String processPlanNumber) {
        this.processPlanNumber = processPlanNumber;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getGtgNumber() {
        return gtgNumber;
    }

    public void setGtgNumber(String gtgNumber) {
        this.gtgNumber = gtgNumber;
    }

    public String getBtnNumber() {
        return btnNumber;
    }

    public void setBtnNumber(String btnNumber) {
        this.btnNumber = btnNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNextOperation() {
        return nextOperation;
    }

    public void setNextOperation(String nextOperation) {
        this.nextOperation = nextOperation;
    }

    public Integer getTrayQuantity() {
        return trayQuantity;
    }

    public void setTrayQuantity(Integer trayQuantity) {
        this.trayQuantity = trayQuantity;
    }

    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }
}
