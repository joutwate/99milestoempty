package com.nnmilestoempty.security;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Custom Spring authentication token that supports username, password and a time based one time password.
 */
public class TOTPAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private static final Logger logger = LoggerFactory.getLogger(TOTPAuthenticationToken.class);

    @Getter
    private Integer oneTimePassword;

    public TOTPAuthenticationToken(Object principal, Object credentials, String oneTimePassword) {
        super(principal, credentials);

        // Convert the one time password string to an integer. We only support one time password formats that include
        // spaces.
        if (oneTimePassword != null) {
            try {
                this.oneTimePassword = Integer.parseInt(oneTimePassword.replaceAll("\\s+", ""));
            } catch (NumberFormatException e) {
                this.oneTimePassword = null;
                logger.error("Unable to parse 2-factor TOTP verification value to an integer as expected");
            }
        }
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();

        this.oneTimePassword = null;
    }
}
