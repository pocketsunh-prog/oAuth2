package com.oauth.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request payload for refreshing an access token using a refresh token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
