package com.crm.controller;

import com.crm.dto.UserRequest;
import com.crm.dto.UserResponse;
import com.crm.entity.User;
import com.crm.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_ADVISOR')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users – request received");
        List<UserResponse> users = userService.findAllAsDto();
        log.debug("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_ADVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        log.info("GET /api/users/{} – request received", id);
        return ResponseEntity.ok(userService.findByIdAsDto(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.info("GET /api/users/me – for user: {}", email);
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(userService.findByIdAsDto(user.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest req) {
        log.info("POST /api/users – creating user with email: {}", req.getEmail());
        UserResponse created = userService.createUser(req);
        log.info("User created with id: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest req) {
        log.info("PUT /api/users/{} – request received", id);
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /api/users/{} – request received", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}