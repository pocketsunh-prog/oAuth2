package com.oauth.server.controller;

import com.oauth.server.dto.*;
import com.oauth.server.model.User;
import com.oauth.server.service.CustomUserDetailsService;
import com.oauth.server.service.TokenStorageService;
import com.oauth.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for authentication operations: login, registration, and profile.
 * <p>
 * The login endpoint validates credentials, issues JWT tokens,
 * and stores them in the database for management.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenStorageService tokenStorageService;

    @Value("${app.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * Register a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse.UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Log in with username and password.
     * Returns a JWT access token and refresh token.
     * Tokens are stored in the database for management.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Load the user
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Verify the password
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Find the user entity
        User user = userService.findByUsername(request.getUsername());

        // Generate tokens
        Instant now = Instant.now();
        String accessToken = "tk_" + UUID.randomUUID().toString().replace("-", "");
        String refreshToken = "rt_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(accessTokenValidity);

        // Store the token in the database for management
        tokenStorageService.storeToken(
                user,
                "frontend-client",
                accessToken,
                refreshToken,
                "Bearer",
                "read write",
                expiresAt
        );

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenValidity)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();

        log.info("User {} logged in successfully", user.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(userInfo);
    }
}
