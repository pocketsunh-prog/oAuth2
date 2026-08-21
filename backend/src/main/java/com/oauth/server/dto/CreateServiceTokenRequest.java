package com.oauth.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request payload for creating a service token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceTokenRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * Comma-separated scopes (e.g., "read,write"). Optional.
     */
    private String scopes;

    /**
     * Number of days until the token expires. Null means the token never expires.
     */
    private Integer expiresInDays;
}
