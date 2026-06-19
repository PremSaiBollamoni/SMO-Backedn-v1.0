package com.cutm.smo.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "loginid is required")
    private String loginid;

    @NotBlank(message = "password is required")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String loginid, String password) {
        this.loginid = loginid;
        this.password = password;
    }

    public String getLoginid() {
        return loginid;
    }

    public void setLoginid(String loginid) {
        this.loginid = loginid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
