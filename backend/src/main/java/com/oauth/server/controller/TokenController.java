package com.oauth.server.controller;

import com.oauth.server.dto.ApiError;
import com.oauth.server.dto.TokenInfo;
import com.oauth.server.model.User;
import com.oauth.server.service.TokenManagerService;
import com.oauth.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing OAuth2 tokens.
 * Allows users to list and revoke their tokens.
 */
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenManagerService tokenManagerService;
    private final UserService userService;

    /**
     * List all tokens for the currently authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<TokenInfo>> listTokens(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<TokenInfo> tokens = tokenManagerService.listTokens(user);
        return ResponseEntity.ok(tokens);
    }

    /**
     * Revoke a specific token by ID.
     */
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> revokeToken(
            @PathVariable Long tokenId,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName());

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        boolean revoked = tokenManagerService.revokeToken(tokenId, user);

        if (!revoked) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Revoke all tokens for the currently authenticated user.
     */
    @DeleteMapping
    public ResponseEntity<Integer> revokeAllTokens(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        int count = tokenManagerService.revokeAllTokens(user);
        return ResponseEntity.ok(count);
    }
}
