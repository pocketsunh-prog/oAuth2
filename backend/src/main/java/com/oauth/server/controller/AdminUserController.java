package com.oauth.server.controller;

import com.oauth.server.dto.AuthResponse;
import com.oauth.server.dto.CreateUserRequest;
import com.oauth.server.model.User;
import com.oauth.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(userInfoList);
    }
}
