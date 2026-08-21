package com.oauth.server.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload describing an OAuth2 token.
 * Used by the token management API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenInfo {

    private Long id;
    private String clientId;
    private String tokenType;
    private String scopes;
    private String accessTokenPreview;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private boolean expired;
}
