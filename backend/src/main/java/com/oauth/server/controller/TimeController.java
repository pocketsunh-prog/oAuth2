package com.oauth.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for returning the current server time.
 * Useful for testing token authentication (user tokens and service tokens).
 */
@RestController
@RequestMapping("/api/time")
@Slf4j
public class TimeController {

    /**
     * Get the current server time.
     * No authentication required — useful for basic health checks.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentTime() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Server is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current server time along with the authenticated user info.
     * Requires authentication — useful for testing token validity.
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> getAuthenticatedTime(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Token is valid");

        if (authentication != null) {
            response.put("principal", authentication.getName());
            response.put("authorities", authentication.getAuthorities().toString());
        }

        return ResponseEntity.ok(response);
    }
}
