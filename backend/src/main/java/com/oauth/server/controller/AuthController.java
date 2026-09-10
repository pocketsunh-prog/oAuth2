package com.oauth.server.controller;

import com.google.zxing.WriterException;
import com.oauth.server.dto.*;
import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
import com.oauth.server.service.CustomUserDetailsService;
import com.oauth.server.service.OtpService;
import com.oauth.server.service.TokenStorageService;
import com.oauth.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for authentication operations: login, registration, profile,
 * and TOTP two-factor authentication management.
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
    private final OtpService otpService;

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
                .totpEnabled(false)
                .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Log in with username and password.
     * <p>
     * If the user has TOTP enabled, returns a temporary token and
     * indicates that OTP verification is required.
     * Otherwise, returns the full auth response with tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Load the user
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Verify the password
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Find the user entity
        User user = userService.findByUsername(request.getUsername());

        // Check if TOTP is enabled
        if (otpService.isTotpEnabled(user)) {
            // Create a temporary token for OTP verification
            String tempToken = otpService.createTempLogin(user.getUsername());

            OtpRequiredResponse otpRequired = OtpRequiredResponse.builder()
                    .tempToken(tempToken)
                    .message("Please enter your 6-digit authenticator code")
                    .build();

            log.info("Login requires OTP for user: {}", user.getUsername());
            return ResponseEntity.ok(otpRequired);
        }

        // No TOTP - generate tokens immediately
        return ResponseEntity.ok(buildAuthResponse(user));
    }

    /**
     * Verify OTP code and complete login.
     * Uses the temporary token from the login step.
     */
    @PostMapping("/otp/verify-login")
    public ResponseEntity<?> verifyOtpLogin(@Valid @RequestBody OtpVerifyRequest request,
                                             @RequestHeader("X-Temp-Token") String tempToken) {
        // Validate the temporary token
        String username = otpService.validateTempLogin(tempToken);
        if (username == null) {
            throw new IllegalArgumentException("Invalid or expired session. Please log in again.");
        }

        // Find the user
        User user = userService.findByUsername(username);
        if (user == null) {
            otpService.removeTempLogin(tempToken);
            throw new IllegalArgumentException("User not found");
        }

        // Verify the OTP code
        if (!otpService.verifyTotpCode(user, request.getCode())) {
            otpService.removeTempLogin(tempToken);
            throw new IllegalArgumentException("Invalid authenticator code");
        }

        // Remove the temp token
        otpService.removeTempLogin(tempToken);

        // Generate and return the auth response
        log.info("OTP login successful for user: {}", user.getUsername());
        return ResponseEntity.ok(buildAuthResponse(user));
    }

    /**
     * Set up TOTP for the current user.
     * Generates a secret key and returns a QR code for scanning.
     */
    @PostMapping("/otp/setup")
    public ResponseEntity<OtpSetupResponse> setupOtp(Authentication authentication)
            throws WriterException, IOException {

        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        OtpSetupResponse setup = otpService.setupTotp(user);
        log.info("TOTP setup initiated for user: {}", user.getUsername());
        return ResponseEntity.ok(setup);
    }

    /**
     * Verify and enable TOTP setup.
     * User provides a code from their authenticator app.
     */
    @PostMapping("/otp/verify-setup")
    public ResponseEntity<?> verifyOtpSetup(@Valid @RequestBody OtpVerifyRequest request,
                                            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        boolean enabled = otpService.verifyAndEnableTotp(user, request.getCode());

        if (!enabled) {
            throw new IllegalArgumentException("Invalid code. Please try again.");
        }

        return ResponseEntity.ok().body("{\"message\":\"Two-factor authentication enabled\"}");
    }

    /**
     * Disable TOTP for the current user.
     */
    @PostMapping("/otp/disable")
    public ResponseEntity<?> disableOtp(Authentication authentication) {

        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        otpService.disableTotp(user);
        return ResponseEntity.ok().body("{\"message\":\"Two-factor authentication disabled\"}");
    }

    /**
     * Check TOTP status for the current user.
     */
    @GetMapping("/otp/status")
    public ResponseEntity<?> getOtpStatus(Authentication authentication) {

        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(
                "{\"totpEnabled\":" + otpService.isTotpEnabled(user) + "}");
    }

    /**
     * Refresh an expired access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        UserToken token = tokenStorageService.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token") {});

        if (token.isRevoked()) {
            throw new AuthenticationException("Refresh token has been revoked") {};
        }

        if (token.getRefreshExpiresAt() != null
                && token.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Refresh token has expired") {};
        }

        String newAccessToken = "tk_" + UUID.randomUUID().toString().replace("-", "");
        String newRefreshToken = "rt_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(accessTokenValidity);

        tokenStorageService.rotateTokens(token, newAccessToken, newRefreshToken, newExpiresAt);

        User user = token.getUser();
        // Build the response WITHOUT calling storeToken — rotateTokens
        // already updated the existing token row in the database.
        AuthResponse response = buildResponseWithoutStoring(user, newAccessToken, newRefreshToken);

        log.info("Tokens refreshed for user {}", user.getUsername());
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
                .totpEnabled(otpService.isTotpEnabled(user))
                .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Build a full auth response with new tokens for the given user.
     * Stores the tokens in the database (used for login / OTP verification).
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = "tk_" + UUID.randomUUID().toString().replace("-", "");
        String refreshToken = "rt_" + UUID.randomUUID().toString().replace("-", "");
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Build a full auth response with specified tokens.
     * Stores the tokens in the database (used for login / OTP verification).
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(accessTokenValidity);

        tokenStorageService.storeToken(
                user,
                "frontend-client",
                accessToken,
                refreshToken,
                "Bearer",
                "read write",
                expiresAt
        );

        return buildResponseWithoutStoring(user, accessToken, refreshToken);
    }

    /**
     * Build an auth response WITHOUT storing tokens in the database.
     * Used by the refresh flow, where rotateTokens already updated the row.
     */
    private AuthResponse buildResponseWithoutStoring(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenValidity)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .totpEnabled(otpService.isTotpEnabled(user))
                        .build())
                .build();
    }
}
