package com.cutm.smo.dto;

public class MergingRequest {
    private String tub1Qr;
    private String tub1Description;
    private String tub2Qr;
    private String tub2Description;
    private Long supervisorId; // Added for enhanced workflow
    private String notes; // Added for enhanced workflow

    // Getters and Setters
    public String getTub1Qr() {
        return tub1Qr;
    }

    public void setTub1Qr(String tub1Qr) {
        this.tub1Qr = tub1Qr;
    }

    public String getTub1Description() {
        return tub1Description;
    }

    public void setTub1Description(String tub1Description) {
        this.tub1Description = tub1Description;
    }

    public String getTub2Qr() {
        return tub2Qr;
    }

    public void setTub2Qr(String tub2Qr) {
        this.tub2Qr = tub2Qr;
    }

    public String getTub2Description() {
        return tub2Description;
    }

    public void setTub2Description(String tub2Description) {
        this.tub2Description = tub2Description;
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
}
