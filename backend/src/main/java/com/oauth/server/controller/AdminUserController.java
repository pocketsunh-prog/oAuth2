package com.oauth.server.controller;

import com.oauth.server.dto.AdminUpdateUserRequest;
import com.oauth.server.dto.AuthResponse;
import com.oauth.server.dto.CreateUserRequest;
import com.oauth.server.model.User;
import com.oauth.server.service.OtpService;
import com.oauth.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for admin user management operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user. Only admins can create users.
     */
    @PostMapping
    public ResponseEntity<AuthResponse.UserInfo> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.registerUser(request, request.getRole());

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .totpEnabled(user.isTotpEnabled())
                .build();

        log.info("Admin created user: {}", user.getUsername());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * List all users in the system. Only admins can view all users.
     */
    @GetMapping
    public ResponseEntity<List<AuthResponse.UserInfo>> listUsers() {
        List<User> users = userService.findAllUsers();

        List<AuthResponse.UserInfo> userInfoList = users.stream()
                .map(user -> AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .totpEnabled(user.isTotpEnabled())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(userInfoList);
    }

    /**
     * Update a user's information. Only admins can update users.
     * Supports updating email, role, password, and TOTP status.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AuthResponse.UserInfo> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {

        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Update email if provided
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        // Update role if provided
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update TOTP status if provided
        if (request.getTotpEnabled() != null) {
            if (request.getTotpEnabled()) {
                // Enable TOTP only if it's not already enabled
                if (!otpService.isTotpEnabled(user)) {
                    user.setTotpEnabled(true);
                    // Note: the admin can't set the secret here — the user must
                    // complete the setup flow. We just mark it as enabled.
                }
            } else {
                // Disable TOTP
                otpService.disableTotp(user);
            }
        }

        userService.save(user);

        log.info("Admin updated user: {} (email={}, role={}, totpEnabled={})",
                user.getUsername(), user.getEmail(), user.getRole(), user.isTotpEnabled());

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .totpEnabled(user.isTotpEnabled())
                .build();

        return ResponseEntity.ok(userInfo);
    }
}
