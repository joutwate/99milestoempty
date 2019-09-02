package com.nnmilestoempty.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

/**
 * Test controller that has endpoints that are accessible only to users with specific roles.
 */
@RestController
public class TestUserAccessController {
    @GetMapping("/user")
    @RolesAllowed({"ADMIN", "USER"})
    public ResponseEntity<String> getUser() {
        return ResponseEntity.ok("Hello user!");
    }

    @GetMapping("/admin")
    @RolesAllowed({"ADMIN"})
    public ResponseEntity<String> getAdmin() {
        return ResponseEntity.ok("Hello admin!");
    }
}
