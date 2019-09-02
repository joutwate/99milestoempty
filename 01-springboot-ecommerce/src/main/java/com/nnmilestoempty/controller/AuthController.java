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
import com.nnmilestoempty.security.CustomUserDetails;
import com.nnmilestoempty.security.CustomUserDetailsManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public AuthController(CustomAuthenticationProvider authenticationProvider,
                          CustomUserDetailsManager userDetailsManager, JwtTokenProvider jwtTokenProvider,
                          RegistrationKeyRepository registrationKeyRepository, JavaMailSender javaMailSender) {
        this.authenticationProvider = authenticationProvider;
        this.userDetailsManager = userDetailsManager;
        this.tokenProvider = jwtTokenProvider;
        this.registrationKeyRepository = registrationKeyRepository;
        this.mailSender = javaMailSender;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> registerUser(@Valid SignUpRequest registrationRequest) {
        // Check to see if username is taken.
        String username = registrationRequest.getUsername().trim();
        if (userDetailsManager.userExists(username)) {
            return ResponseEntity.badRequest().body("Username is already taken");
        }

        // Create disabled user.
        Role role = new Role("ROLE_USER");
        User user = new User(registrationRequest.getFirstName().trim(), registrationRequest.getLastName().trim(),
                registrationRequest.getEmail(), username, registrationRequest.getPassword(), false);
        role.setUser(user);
        user.setRoles(Collections.singleton(role));
        UserDetails details = new CustomUserDetails(user);
        userDetailsManager.createUser(details);

        // Create a registration key for this user.
        RegistrationToken registrationToken = new RegistrationToken(username);
        registrationKeyRepository.save(registrationToken);

        // Send email with unique id that is valid for 10 minutes for user to register.
        // Move this to an asynchronous process
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(registrationRequest.getEmail());
        msg.setFrom("registration@99milestoempty.com");
        msg.setSubject("Welcome!");
        msg.setText("Hello World \n Spring Boot Email.\n Please verify your account by visiting " +
                "/api/auth/verifyRegistration/" + registrationToken.getToken());
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
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username or password is incorrect");
        }
    }
}
