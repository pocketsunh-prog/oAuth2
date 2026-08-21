package com.oauth.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents an OAuth2 token issued to a user.
 * Used for token management (list, revoke).
 */
@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "access_token", nullable = false, unique = true, length = 500)
    private String accessToken;

    @Column(name = "refresh_token", unique = true, length = 500)
    private String refreshToken;

    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType;

    @Column(name = "scopes", length = 500)
    private String scopes;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
