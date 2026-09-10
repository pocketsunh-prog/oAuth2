package com.oauth.server.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Service for Time-based One-Time Password (TOTP) operations.
 * <p>
 * Implements RFC 6238 (TOTP) and RFC 4226 (HOTP) for compatibility
 * with Google Authenticator and similar apps.
 */
@Service
@Slf4j
public class TotpService {

    /** Number of digits in the generated code. */
    private static final int CODE_DIGITS = 6;

    /** Time step in seconds (30 seconds is the standard). */
    private static final int TIME_STEP_SECONDS = 30;

    /** Number of time steps to check before/after current (for clock drift). */
    private static final int ALLOWED_TIME_DRIFT_STEPS = 1;

    /** HMAC algorithm used for TOTP. */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final String issuer;

    public TotpService(@Value("${app.issuer-uri}") String issuer) {
        this.issuer = issuer;
    }

    /**
     * Generate a new random TOTP secret key.
     *
     * @return Base32-encoded secret key
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits, standard for TOTP
        random.nextBytes(bytes);

        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    /**
     * Generate a TOTP code for the given secret at the current time.
     *
     * @param secret Base32-encoded secret key
     * @return 6-digit TOTP code
     */
    public String generateCode(String secret) {
        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return generateCodeForTime(secret, timeIndex);
    }

    /**
     * Verify a TOTP code against a secret.
     * Allows for a small amount of clock drift (±1 time step).
     *
     * @param secret   Base32-encoded secret key
     * @param userCode The code entered by the user
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, String userCode) {
        if (secret == null || userCode == null || userCode.length() != CODE_DIGITS) {
            return false;
        }

        long timeIndex = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        // Check current time step and allowed drift steps
        for (int i = -ALLOWED_TIME_DRIFT_STEPS; i <= ALLOWED_TIME_DRIFT_STEPS; i++) {
            String expectedCode = generateCodeForTime(secret, timeIndex + i);
            if (constantTimeEquals(expectedCode, userCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate a QR code for the TOTP setup URI.
     * The URI follows the otpauth:// format used by Google Authenticator.
     *
     * @param username The user's username
     * @param secret   The TOTP secret
     * @return PNG image bytes of the QR code
     */
    public byte[] generateQrCode(String username, String secret) throws WriterException, IOException {
        String uri = buildOtpauthUri(username, secret);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(uri, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Build the otpauth:// URI for QR code generation.
     * Format: otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
     */
    public String buildOtpauthUri(String username, String secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer,
                encodedUsername,
                secret,
                encodedIssuer,
                CODE_DIGITS,
                TIME_STEP_SECONDS
        );
    }

    /**
     * Generate a TOTP code for a specific time index.
     */
    private String generateCodeForTime(String secret, long timeIndex) {
        try {
            Base32 base32 = new Base32();
            byte[] key = base32.decode(secret);
            byte[] data = new byte[8];

            // Convert time index to big-endian 8-byte array
            long value = timeIndex;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            // Compute HMAC-SHA1
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec signKey = new SecretKeySpec(key, HMAC_ALGORITHM);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);

            // Pad with leading zeros
            return String.format("%0" + CODE_DIGITS + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate TOTP code", e);
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
