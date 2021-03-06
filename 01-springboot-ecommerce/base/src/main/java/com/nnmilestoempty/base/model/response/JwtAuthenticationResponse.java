package com.nnmilestoempty.base.model.response;

import lombok.Getter;
import lombok.Setter;

public class JwtAuthenticationResponse {
    @Getter
    @Setter
    private String accessToken;

    @Getter
    private String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
