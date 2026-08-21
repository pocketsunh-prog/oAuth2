package com.oauth.server.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload describing a service token.
 * Used by the admin service token management API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceTokenInfo {

    private Long id;
    private String name;
    private String tokenPreview;
    private String scopes;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private boolean expired;
}
