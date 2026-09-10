package com.oauth.server.dto;

import lombok.*;

/**
 * Response payload when OTP verification is required during login.
 * Indicates that the user must provide a TOTP code to complete authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequiredResponse {

    /** A temporary token that must be used to submit the OTP code. */
    private String tempToken;

    /** Message indicating OTP is required. */
    private String message;
}
