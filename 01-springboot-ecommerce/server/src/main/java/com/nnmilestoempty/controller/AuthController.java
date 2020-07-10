package com.nnmilestoempty.controller;

import com.nnmilestoempty.config.security.JwtTokenProvider;
import com.nnmilestoempty.model.dao.auth.Role;
import com.nnmilestoempty.model.dao.auth.User;
import com.nnmilestoempty.model.dao.auth.RegistrationToken;
import com.nnmilestoempty.model.request.MultiFactorPreferenceRequest;
import com.nnmilestoempty.model.response.JwtAuthenticationResponse;
import com.nnmilestoempty.model.request.LoginRequest;
import com.nnmilestoempty.model.request.SignUpRequest;
import com.nnmilestoempty.repository.auth.RegistrationKeyRepository;
import com.nnmilestoempty.security.CustomAuthenticationProvider;
import com.nnmilestoempty.security.CustomUserDetails;
import com.nnmilestoempty.security.CustomUserDetailsManager;
import com.nnmilestoempty.security.TOTPAuthenticationToken;
import com.nnmilestoempty.service.EmailService;
import com.nnmilestoempty.utils.ResponseUtils;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController implements ApplicationEventPublisherAware {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final CustomAuthenticationProvider authenticationProvider;
    private final CustomUserDetailsManager userDetailsManager;
    private final JwtTokenProvider tokenProvider;
    private final RegistrationKeyRepository registrationKeyRepository;
    private final GoogleAuthenticator googleAuthenticator;
    private final EmailService emailService;
    private ApplicationEventPublisher applicationEventPublisher;

    public AuthController(CustomAuthenticationProvider authenticationProvider,
                          CustomUserDetailsManager userDetailsManager, JwtTokenProvider jwtTokenProvider,
                          RegistrationKeyRepository registrationKeyRepository, GoogleAuthenticator googleAuthenticator,
                          EmailService emailService) {
        this.authenticationProvider = authenticationProvider;
        this.userDetailsManager = userDetailsManager;
        this.tokenProvider = jwtTokenProvider;
        this.registrationKeyRepository = registrationKeyRepository;
        this.googleAuthenticator = googleAuthenticator;
        this.emailService = emailService;
    }

    @PostMapping(value = "/register")
    @Transactional
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest registrationRequest) {
        // Check to see if username is taken, if it's not available fail right away.
        String username = registrationRequest.getUsername().trim();
        String emailAddress = registrationRequest.getEmail().trim();
        if (userDetailsManager.userExists(username)) {
            Map<String, Object> responseBody =
                    ResponseUtils.createErrorsMap("Username is unavailable", "signUpRequest", "username");
            return ResponseEntity.badRequest().body(responseBody);
        }

        // Configure a user.
        Role role = new Role("ROLE_USER");
        User user = new User(registrationRequest.getFirstName().trim(), registrationRequest.getLastName().trim(),
                emailAddress, username, registrationRequest.getPassword(), false);
        role.setUser(user);
        user.setRoles(Collections.singleton(role));
        UserDetails details = new CustomUserDetails(user);

        // Don't enable 2-factor auth until they verify via a generated code
        user.setUsing2FA(false);

        // Create the disabled user.
        userDetailsManager.createUser(details);

        // Create a registration key for this user.
        RegistrationToken registrationToken = new RegistrationToken(username);
        registrationKeyRepository.save(registrationToken);

        // Send email with unique id that is valid for a system-configured number of minutes for user to register
        // TODO: Move this to an asynchronous process
        Map<String, String> map = new HashMap<>();
        map.put("firstName", user.getFirstName());
        map.put("registrationToken", registrationToken.getToken());
        emailService
                .sendEmail(emailAddress, "no-reply@99milestoempty.com", "Complete your registration at 99 Miles to Empty",
                        map, EmailService.EmailTemplate.CONFIRM_REGISTRATION);

        return ResponseEntity.ok("Email sent");
    }

    @GetMapping(value = "/register/{registrationToken}")
    public ResponseEntity<?> verifyRegistration(@PathVariable String registrationToken) {
        // Check if the registration key is valid
        RegistrationToken result = registrationKeyRepository.findFirstByTokenOrderByCreatedDesc(registrationToken);
        // If we find a registration key and it isn't expired and hasn't been used already.
        if (result != null && !result.isExpired() && result.getConsumed() == null) {
            String username = result.getUsername();
            CustomUserDetails userDetails;
            try {
                userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                logger.error("Registration key was valid but no user found", e);
                return ResponseEntity.badRequest().body("Invalid registration token");
            }

            // Check if the user has already been activated.
            if (userDetails.isEnabled()) {
                // This user is already enabled, consume the key and log an error.
                logger.error("Received a valid registration key for an already enabled user, marking it as consumed.");
                result.setConsumed(LocalDateTime.now());
                registrationKeyRepository.save(result);
                return ResponseEntity.badRequest().body("User is already verified.");
            } else {
                // Enable this user.
                User user = userDetails.getUser();
                user.setEnabled(true);
                userDetailsManager.updateUser(userDetails);

                // Consume the registration key.
                result.setConsumed(LocalDateTime.now());
                registrationKeyRepository.save(result);

                // Send a successful registration email to the user.
                // TODO: Move this to an asynchronous process
                Map<String, String> map = new HashMap<>();
                map.put("firstName", user.getFirstName());
                emailService.sendEmail(user.getEmail(), "no-reply@99milestoempty.com", "Welcome to 99 Miles to Empty!", map,
                        EmailService.EmailTemplate.REGISTRATION_SUCCESSFUL);

                return ResponseEntity.ok().build();
            }
        }
        // No registration key was found for the request or is was already used or expired.
        return ResponseEntity.badRequest().build();
    }

    @GetMapping(value = "/user/preferences")
    public ResponseEntity<?> isMultiFactorEnabled() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(username);
        Map<String, Object> response = new HashMap<>();
        response.put("multiFactorEnabled", userDetails.isUsing2FA());
        return ResponseEntity.ok().body(response);
    }

    @PostMapping(value = "/user/multifactorauth")
    public ResponseEntity<?> updateMultiFactorAuth(@Valid @RequestBody MultiFactorPreferenceRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(username);
        User user = userDetails.getUser();

        TOTPAuthenticationToken authToken = new TOTPAuthenticationToken(username, request.getPassword(),
                request.getVerificationCode());

        // Password must be valid to update any security setting.
        if (!authenticationProvider.authenticatePassword(authToken)) {
            return ResponseEntity.badRequest().build();
        }

        // User is in the process of setting up multi-factor auth. Validate their verification code and enable
        // 2-factor auth or return a bad request response.
        if (request.isEnable2FA() && !userDetails.isUsing2FA() && userDetails.getSecret2FA() != null &&
                authToken.getOneTimePassword() != null) {
            // Get the verification code they sent and verify it.
            if (!googleAuthenticator.authorize(userDetails.getSecret2FA(), authToken.getOneTimePassword())) {
                return ResponseEntity.badRequest().build();
            }

            // Go ahead and update multi-factor setting based on request.
            user.setUsing2FA(true);
            userDetailsManager.updateUser(userDetails);

            Map<String, String> map = new HashMap<>();
            map.put("firstName", user.getFirstName());
            emailService
                    .sendEmail(user.getEmail(), "no-reply@99milestoempty.com", "99 Miles to Empty: Multi-factor Enabled", map,
                            EmailService.EmailTemplate.MULTI_FACTOR_ENABLED);

            return ResponseEntity.ok().build();

            // TODO: Send success email for setting up multi-factor auth.
        }

        // If the request is to enable multi factor, verify that the user doesn't already have it enabled,
        // generate a new secret token and return it. A subsequent call to this endpoint will be made to verify
        // that the user can generate a verification code from that secret and then we will enable MFA.
        if (request.isEnable2FA() && !user.isUsing2FA()) {
            if (user.getSecret2FA() == null) {
                // User does not have multi-factor auth and is requesting to enable it.

                // Create a TOTP secret that can be used for two-factor authentication.
                GoogleAuthenticatorKey googleAuthenticatorKey = googleAuthenticator.createCredentials();
                user.setSecret2FA(googleAuthenticatorKey.getKey());
                // Save the changes
                userDetailsManager.updateUser(userDetails);

                Map<String, String> map = new HashMap<>();
                map.put("firstName", user.getFirstName());
                map.put("email", user.getEmail());
                map.put("secret", user.getSecret2FA());

                // TODO: Send email to let user know that a request has been made to enable multi factor auth for their account.

                return ResponseEntity.ok(map);
            }
        } else if (!request.isEnable2FA()) {
            // Disable multi-factor auth.
            user.setSecret2FA(null);
            user.setUsing2FA(false);

            userDetailsManager.updateUser(userDetails);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @PostMapping(value = "/login")
    public ResponseEntity<?> authenticateUser(HttpServletRequest request,
                                              @Valid @RequestBody LoginRequest loginRequest) {
        // Check to see if the user requires two factor authentication.
        CustomUserDetails userDetails;
        try {
            userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(loginRequest.getUsername());
        } catch (UsernameNotFoundException e) {
            publishEvent(loginRequest.getUsername(), "FAILED_LOGIN_NO_SUCH_USER",
                    "ip=" + request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtils.createErrorsMap("Credentials are incorrect"));
        }

        if (userDetails.isUsing2FA() && (loginRequest.getVerificationCode() == null ||
                loginRequest.getVerificationCode().isEmpty())) {
            publishEvent(loginRequest.getUsername(), "FAIL_MULTI_FACTOR_PRECHECK", "ip=" + request.getRemoteAddr());
            Map<String, Object> responseBody =
                    ResponseUtils.createErrorsMap("Mutli-factor authentication is required for login",
                            "loginRequest", "verificationCode");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        }

        Authentication authentication = authenticationProvider.authenticate(
                new TOTPAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword(),
                        loginRequest.getVerificationCode()
                )
        );

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtils.createErrorsMap("Credentials are incorrect"));
        }
    }

    private void publishEvent(String principal, String type, String... data) {
        applicationEventPublisher.publishEvent(new AuditApplicationEvent(principal, type, data));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
