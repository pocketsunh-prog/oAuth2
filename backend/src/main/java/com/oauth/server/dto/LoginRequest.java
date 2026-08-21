package com.oauth.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request payload for user login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
