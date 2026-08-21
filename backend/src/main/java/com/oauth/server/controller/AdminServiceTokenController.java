package com.oauth.server.controller;

import com.oauth.server.dto.CreateServiceTokenRequest;
import com.oauth.server.dto.ServiceTokenInfo;
import com.oauth.server.model.ServiceToken;
import com.oauth.server.model.User;
import com.oauth.server.service.ServiceTokenService;
import com.oauth.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin service token management operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/service-tokens")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceTokenController {

    private final ServiceTokenService serviceTokenService;
    private final UserService userService;

    /**
     * Create a new service token. Only admins can create service tokens.
     * Returns the full token value once (it cannot be retrieved again later).
     */
    @PostMapping
    public ResponseEntity<ServiceTokenInfo> createServiceToken(
            @Valid @RequestBody CreateServiceTokenRequest request,
            Authentication authentication) {

        User admin = userService.findByUsername(authentication.getName());
        ServiceToken serviceToken = serviceTokenService.createToken(request, admin);

        // Build the response with the full token value (only time it's shown)
        ServiceTokenInfo info = ServiceTokenInfo.builder()
                .id(serviceToken.getId())
                .name(serviceToken.getName())
                .tokenPreview(serviceToken.getToken()) // Full token, not masked
                .scopes(serviceToken.getScopes())
                .issuedBy(admin.getUsername())
                .issuedAt(serviceToken.getIssuedAt())
                .expiresAt(serviceToken.getExpiresAt())
                .revoked(false)
                .expired(false)
                .build();

        log.info("Admin {} created service token: {}", admin.getUsername(), request.getName());
        return ResponseEntity.ok(info);
    }

    /**
     * List all service tokens. Only admins can view all service tokens.
     */
    @GetMapping
    public ResponseEntity<List<ServiceTokenInfo>> listServiceTokens() {
        List<ServiceTokenInfo> tokens = serviceTokenService.listTokens();
        return ResponseEntity.ok(tokens);
    }

    /**
     * Revoke a service token. Only admins can revoke service tokens.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeServiceToken(@PathVariable Long id) {
        boolean revoked = serviceTokenService.revokeToken(id);

        if (!revoked) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
