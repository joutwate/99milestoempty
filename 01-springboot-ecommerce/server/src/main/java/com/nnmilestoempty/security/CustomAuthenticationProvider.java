package com.nnmilestoempty.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
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
public class CustomAuthenticationProvider implements AuthenticationProvider, ApplicationEventPublisherAware {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final PasswordEncoder passwordEncoder;

    private final CustomUserDetailsManager userDetailsManager;
    private final GoogleAuthenticator googleAuthenticator;
    private final HttpServletRequest request;
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public CustomAuthenticationProvider(CustomUserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder,
                                        GoogleAuthenticator googleAuthenticator, HttpServletRequest request) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthenticator = googleAuthenticator;
        this.request = request;
    }

    public boolean authenticatePassword(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        CustomUserDetails userDetails;

        try {
            userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(name);
        } catch (UsernameNotFoundException e) {
            logger.warn("Failed login for unknown user '{}'", name);
            publishEvent(name, "FAILED_REVERIFICATION_NO_SUCH_USER", "ip=" + request.getRemoteAddr());
            return false;
        }

        boolean result = false;
        if (userDetails.isEnabled()) {
            // Check to see if the password matches.
            result = passwordEncoder.matches(password, userDetails.getPassword());
            if (!result) {
                publishEvent(name, "FAILED_REVERIFICATION", "ip=" + request.getRemoteAddr());
            }
        }

        return result;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        CustomUserDetails userDetails;
        try {
            userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(name);
        } catch (UsernameNotFoundException e) {
            // This code should never get executed since there is always a pre-check in the controller.
            publishEvent(name, "FAILED_LOGIN_NO_SUCH_USER", "ip=" + request.getRemoteAddr());
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
                    Integer verificationToken = customAuthenticationToken.getOneTimePassword();

                    if (verificationToken == null) {
                        publishEvent(authentication.getPrincipal().toString(), "FAILED_MULTI_FACTOR_LOGIN",
                                "ip=" + request.getRemoteAddr());
                        String log = String.format(
                                "Failed 2-factor login for user '%s' from %s. No verification token present.", name,
                                request.getRemoteAddr());
                        logger.warn(log);
                        return null;
                    }

                    boolean isVerificationTokenValid =
                            googleAuthenticator.authorize(userDetails.getSecret2FA(), verificationToken);

                    if (isVerificationTokenValid) {
                        Collection<? extends GrantedAuthority> grantedAuthorities = userDetails.getAuthorities();
                        result = new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuthorities);
                        publishEvent(authentication.getPrincipal().toString(), "SUCCESSFUL_MULTI_FACTOR_LOGIN",
                                "ip=" + request.getRemoteAddr());
                        logger.info("Successful 2-factor auth login for user '{}' from {}", name,
                                request.getRemoteAddr());
                    } else {
                        publishEvent(authentication.getPrincipal().toString(), "FAILED_MULTI_FACTOR_LOGIN",
                                "ip=" + request.getRemoteAddr());
                        logger.warn("Failed 2-factor login for user '{}' from {}", name, request.getRemoteAddr());
                    }
                } else {
                    Collection<? extends GrantedAuthority> grantedAuthorities = userDetails.getAuthorities();
                    result = new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuthorities);
                    publishEvent(authentication.getPrincipal().toString(), "SUCCESSFUL_LOGIN",
                            "ip=" + request.getRemoteAddr());
                    logger.warn("Successful login for user '{}' from {}", name, request.getRemoteAddr());
                }
            } else {
                publishEvent(authentication.getPrincipal().toString(), "FAILED_LOGIN",
                        "ip=" + request.getRemoteAddr());
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

    private void publishEvent(String principal, String type, String... data) {
        applicationEventPublisher.publishEvent(new AuditApplicationEvent(principal, type, data));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}