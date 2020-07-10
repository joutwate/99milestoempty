package com.nnmilestoempty.model.request;

import javax.validation.constraints.NotBlank;

/**
 * Define a login request to our site along with validations.
 */
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String verificationCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
