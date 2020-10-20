package com.nnmilestoempty.base.model.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * Define a login request to our site along with validations.
 */
public class LoginRequest {
    @NotBlank
    @Getter
    @Setter
    private String username;

    @NotBlank
    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private String verificationCode;
}
