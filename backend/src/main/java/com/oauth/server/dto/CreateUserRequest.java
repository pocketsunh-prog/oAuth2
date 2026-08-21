package com.oauth.server.dto;

import lombok.*;

/**
 * Request payload for admin-created users.
 * Extends RegisterRequest with an optional role field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateUserRequest extends RegisterRequest {

    /**
     * The role to assign to the user. Defaults to "USER" if not specified.
     * Valid values: "USER", "ADMIN".
     */
    private String role;
}
