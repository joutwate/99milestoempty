package com.nnmilestoempty.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * Our custom implementation of the authentication provider that implements authenticating a user by their credentials.
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final PasswordEncoder passwordEncoder;

    private CustomUserDetailsManager userDetailsManager;
    private GoogleAuthenticator googleAuthenticator;
    private HttpServletRequest request;

    @Autowired
    public CustomAuthenticationProvider(CustomUserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder,
                                        GoogleAuthenticator googleAuthenticator, HttpServletRequest request) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthenticator = googleAuthenticator;
        this.request = request;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        CustomUserDetails userDetails;

        try {
            userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(name);
        } catch (UsernameNotFoundException e) {
            logger.warn("Failed login for unknown user '{}'", name);
            return null;
        }

        Authentication result = null;
        if (userDetails.isEnabled()) {
            // Check to see if the password matches.
            if (passwordEncoder.matches(password, userDetails.getPassword())) {
                // Check to see if this user requires 2-factor authentication
                if (userDetails.isUsing2FA()) {
                    // Validate the verification code.
                    TOTPAuthenticationToken customAuthenticationToken = (TOTPAuthenticationToken) authentication;
                    int verificationToken = customAuthenticationToken.getOneTimePassword();

                    boolean isVerificationTokenValid =
                            googleAuthenticator.authorize(userDetails.getSecret2FA(), verificationToken);

                    if (isVerificationTokenValid) {
                        Collection<? extends GrantedAuthority> grantedAuthorities = userDetails.getAuthorities();
                        result = new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuthorities);
                        logger.info("Successful 2-factor auth login for user '{}' from {}", name,
                                request.getRemoteAddr());
                    } else {
                        logger.warn("Failed 2-factor login for user '{}' from {}", name, request.getRemoteAddr());
                    }
                } else {
                    Collection<? extends GrantedAuthority> grantedAuthorities = userDetails.getAuthorities();
                    result = new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuthorities);
                    logger.warn("Successful login for user '{}' from {}", name, request.getRemoteAddr());
                }
            } else {
                logger.warn("Failed login for user '{}' from {}", name, request.getRemoteAddr());
            }
        } else {
            logger.warn("Failed login for disabled user '{}' from {}", name, request.getRemoteAddr());
        }

        return result;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
