package com.oauth.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user in the system.
 * Users can authenticate and own OAuth2 tokens.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled;

    @Builder.Default
    @Column(name = "role", nullable = false, length = 20)
    private String role = "USER";

    /** TOTP secret key (Base32-encoded). Null if 2FA is not enabled. */
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    /** Whether TOTP two-factor authentication is enabled. */
    @Builder.Default
    @Column(name = "totp_enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean totpEnabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
