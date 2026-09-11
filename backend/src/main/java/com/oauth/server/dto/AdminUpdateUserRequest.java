package com.oauth.server.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Request payload for admin updating a user.
 * All fields are optional — only provided fields are updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    /** New email address (optional). */
    @Email(message = "Must be a valid email address")
    private String email;

    /** New role (optional). */
    private String role;

    /** New password (optional). */
    private String password;

    /** Enable or disable TOTP (optional). */
    private Boolean totpEnabled;
}
