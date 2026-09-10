package com.oauth.server.dto;

import lombok.*;

/**
 * Response payload for TOTP setup.
 * Contains the secret key, otpauth URI, and QR code image (Base64).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSetupResponse {

    /** Base32-encoded TOTP secret key. */
    private String secret;

    /** otpauth:// URI for QR code generation. */
    private String otpauthUri;

    /** Base64-encoded PNG QR code image. */
    private String qrCodeBase64;

    /** Whether TOTP is already enabled for this user. */
    private boolean alreadyEnabled;
}
