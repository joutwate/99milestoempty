package com.nnmilestoempty.controller;

import com.nnmilestoempty.config.security.JwtTokenProvider;
import com.nnmilestoempty.model.auth.Role;
import com.nnmilestoempty.model.auth.User;
import com.nnmilestoempty.model.dao.auth.RegistrationToken;
import com.nnmilestoempty.payload.JwtAuthenticationResponse;
import com.nnmilestoempty.payload.LoginRequest;
import com.nnmilestoempty.payload.SignUpRequest;
import com.nnmilestoempty.repository.auth.RegistrationKeyRepository;
import com.nnmilestoempty.security.CustomAuthenticationProvider;
import com.nnmilestoempty.security.TOTPAuthenticationToken;
import com.nnmilestoempty.security.CustomUserDetails;
import com.nnmilestoempty.security.CustomUserDetailsManager;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private CustomAuthenticationProvider authenticationProvider;
    private CustomUserDetailsManager userDetailsManager;
    private JwtTokenProvider tokenProvider;
    private RegistrationKeyRepository registrationKeyRepository;
    private JavaMailSender mailSender;
    private GoogleAuthenticator googleAuthenticator;

    public AuthController(CustomAuthenticationProvider authenticationProvider,
                          CustomUserDetailsManager userDetailsManager, JwtTokenProvider jwtTokenProvider,
                          RegistrationKeyRepository registrationKeyRepository, JavaMailSender javaMailSender,
                          GoogleAuthenticator googleAuthenticator) {
        this.authenticationProvider = authenticationProvider;
        this.userDetailsManager = userDetailsManager;
        this.tokenProvider = jwtTokenProvider;
        this.registrationKeyRepository = registrationKeyRepository;
        this.mailSender = javaMailSender;
        this.googleAuthenticator = googleAuthenticator;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> registerUser(@Valid SignUpRequest registrationRequest) {
        // Check to see if username is taken.
        String username = registrationRequest.getUsername().trim();
        String emailAddress = registrationRequest.getEmail().trim();
        if (userDetailsManager.userExists(username)) {
            return ResponseEntity.badRequest().body("Username is already taken");
        }

        // Configure a user.
        Role role = new Role("ROLE_USER");
        User user = new User(registrationRequest.getFirstName().trim(), registrationRequest.getLastName().trim(),
                emailAddress, username, registrationRequest.getPassword(), false);
        role.setUser(user);
        user.setRoles(Collections.singleton(role));
        UserDetails details = new CustomUserDetails(user);

        // Create a TOTP secret that can be used for two-factor authentication.
        GoogleAuthenticatorKey googleAuthenticatorKey = googleAuthenticator.createCredentials();
        user.setSecret2FA(googleAuthenticatorKey.getKey());

        // Create the disabled user.
        userDetailsManager.createUser(details);

        // Create a registration key for this user.
        RegistrationToken registrationToken = new RegistrationToken(username);
        registrationKeyRepository.save(registrationToken);

        // Send email with unique id that is valid for 10 minutes for user to register.
        // Move this to an asynchronous process
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailAddress);
        msg.setFrom("registration@99milestoempty.com");
        msg.setSubject("Welcome!");
        StringBuilder body = new StringBuilder();
        body.append("Hello World \n Spring Boot Email.\n Please verify your account by visiting " +
                "/api/auth/verifyRegistration/").append(registrationToken.getToken());
        body.append("\n\nYour two factor authentication secret is: ").append(user.getSecret2FA());
        body.append("\nYou can also scan this QR code into Google Authenticator\n");
        String format = "https://www.google.com/chart?chs=200x200&cht=qr&chl=otpauth://totp/%s:%s?secret=%s&issuer=%s";
        body.append(String.format(format, "99_Miles_to_Empty", emailAddress, user.getSecret2FA(), "99%20Miles%20to%20Empty"));

        body.append("\n\nYour backup codes are:\n");
        for (int scratchCode : googleAuthenticatorKey.getScratchCodes()) {
            body.append(scratchCode).append("\n");
        }
        msg.setText(body.toString());

        mailSender.send(msg);

        return ResponseEntity.ok("Email sent");
    }

    @GetMapping(value = "/verifyRegistration/{registrationToken}")
    public ResponseEntity<?> verifyRegistration(@PathVariable String registrationToken) {
        // Check if the registration key is valid
        RegistrationToken result = registrationKeyRepository.findFirstByTokenOrderByCreatedDesc(registrationToken);
        // If we find a registration key and it hasn't been used already.
        if (result != null && result.getConsumed() == null) {
            String username = result.getUsername();
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsManager.loadUserByUsername(username);
            // Check if the user has already been activated.
            if (userDetails.isEnabled()) {
                // This user is already enabled.
                // Should we consume this registration key, leave it or mark it as invalid?
                return ResponseEntity.badRequest().body("User is already verified.");
            } else {
                // Enable this user.
                userDetails.getUser().setEnabled(true);
                userDetailsManager.updateUser(userDetails);

                // Consume the registration key.
                result.setConsumed(LocalDateTime.now());
                registrationKeyRepository.save(result);

                return ResponseEntity.ok().body("User is now verified.");
            }
        }
        // No registration key was found for the request or is was already used.
        return ResponseEntity.badRequest().build();
    }

    @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> authenticateUser(@Valid LoginRequest loginRequest) {
        // Check to see if the user requires two factor authentication.
        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsManager.loadUserByUsername(loginRequest.getUsername());
        if (userDetails.isUsing2FA() && loginRequest.getVerificationCode().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("2-factor authentication is required for login");
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credentials are incorrect");
        }
    }
}
