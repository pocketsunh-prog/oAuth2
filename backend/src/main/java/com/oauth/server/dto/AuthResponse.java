package com.oauth.server.dto;

import lombok.*;

/**
 * Response payload after successful authentication.
 * Returns a session token and user profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    /**
     * Simplified user profile returned to the client.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
    }
}
