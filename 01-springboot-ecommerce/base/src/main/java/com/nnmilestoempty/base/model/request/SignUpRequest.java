package com.nnmilestoempty.base.model.request;

import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Defines a request to register on our site.
 */
public class SignUpRequest {
    @Getter
    @NotBlank(message = "firstName.notBlank")
    @Size(min = 1, max = 40, message = "firstName.length")
    private String firstName;

    @Getter
    @NotBlank(message = "lastName.notBlank")
    @Size(min = 1, max = 40, message = "lastName.length")
    private String lastName;

    @Getter
    @NotBlank(message = "username.notBlank")
    @Size(min = 3, max = 15, message = "username.length")
    private String username;

    @Getter
    @NotBlank(message = "email.notBlank")
    @Email(message = "email.valid")
    @Size(max = 254, message = "email.length")
    private String email;

    @Getter
    @NotBlank(message = "password.notBlank")
    @Size(min = 14, max = 64, message = "password.length")
    private String password;
}
