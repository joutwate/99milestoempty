package com.nnmilestoempty.base.model.request;

import lombok.Getter;
import lombok.Setter;

public class MultiFactorPreferenceRequest {
    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private String verificationCode;

    @Getter
    @Setter
    private boolean enable2FA;
}
