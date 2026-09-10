package com.oauth.otp

import org.apache.commons.codec.binary.Base32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Time-based One-Time Password (TOTP) generator.
 *
 * Implements RFC 6238 (TOTP) and RFC 4226 (HOTP) for compatibility
 * with Google Authenticator and similar apps.
 *
 * Usage:
 *   val generator = TotpGenerator()
 *   val code = generator.generateCode("JBSWY3DPEHPK3PXP") // returns "123456"
 *   val uri = generator.buildOtpauthUri("user@example.com", "JBSWY3DPEHPK3PXP")
 */
class TotpGenerator {

    companion object {
        /** Number of digits in the generated code. */
        private const val CODE_DIGITS = 6

        /** Time step in seconds (30 seconds is the standard). */
        private const val TIME_STEP_SECONDS = 30L

        /** HMAC algorithm used for TOTP. */
        private const val HMAC_ALGORITHM = "HmacSHA1"

        /** Secure random for generating secrets. */
        private val secureRandom = java.security.SecureRandom()
    }

    /**
     * Generate a new random TOTP secret key.
     *
     * @param length Number of bytes (default 20 = 160 bits, standard for TOTP)
     * @return Base32-encoded secret key
     */
    fun generateSecret(length: Int = 20): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base32().encodeToString(bytes)
    }

    /**
     * Generate a TOTP code for the given secret at the current time.
     *
     * @param secret Base32-encoded secret key
     * @return 6-digit TOTP code
     */
    fun generateCode(secret: String): String {
        val timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS
        return generateCodeForTime(secret, timeIndex)
    }

    /**
     * Generate a TOTP code for a specific time index.
     */
    private fun generateCodeForTime(secret: String, timeIndex: Long): String {
        val key = Base32().decode(secret)
        val data = ByteArray(8)

        // Convert time index to big-endian 8-byte array
        var value = timeIndex
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        // Compute HMAC-SHA1
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val signKey = SecretKeySpec(key, HMAC_ALGORITHM)
        mac.init(signKey)
        val hash = mac.doFinal(data)

        // Dynamic truncation
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % Math.pow(10.0, CODE_DIGITS.toDouble()).toInt()

        // Pad with leading zeros
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    /**
     * Verify a TOTP code against a secret.
     * Allows for a small amount of clock drift (±1 time step).
     *
     * @param secret   Base32-encoded secret key
     * @param userCode The code entered by the user
     * @return true if the code is valid
     */
    fun verifyCode(secret: String, userCode: String): Boolean {
        if (secret.isBlank() || userCode.length != CODE_DIGITS) {
            return false
        }

        val timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS

        // Check current time step and allowed drift steps
        for (i in -1..1) {
            val expectedCode = generateCodeForTime(secret, timeIndex + i)
            if (constantTimeEquals(expectedCode, userCode)) {
                return true
            }
        }

        return false
    }

    /**
     * Build the otpauth:// URI for QR code generation.
     * Format: otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
     */
    fun buildOtpauthUri(account: String, secret: String, issuer: String = "OAuth2Server"): String {
        return "otpauth://totp/${issuer}:${account}" +
                "?secret=${secret}" +
                "&issuer=${issuer}" +
                "&algorithm=SHA1" +
                "&digits=${CODE_DIGITS}" +
                "&period=${TIME_STEP_SECONDS}"
    }

    /**
     * Parse an otpauth:// URI and extract the secret and account.
     */
    fun parseOtpauthUri(uri: String): TotpAccount? {
        if (!uri.startsWith("otpauth://totp/")) {
            return null
        }

        try {
            val pathPart = uri.removePrefix("otpauth://totp/")
            val queryIndex = pathPart.indexOf('?')
            if (queryIndex == -1) return null

            val label = pathPart.substring(0, queryIndex)
            val query = pathPart.substring(queryIndex + 1)

            val params = query.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                key to value
            }

            val secret = params["secret"] ?: return null
            val account = label.substringAfterLast(":", label)

            return TotpAccount(account = account, secret = secret)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get the remaining seconds in the current time step.
     * Used for the countdown display.
     */
    fun getRemainingSeconds(): Int {
        val elapsed = (System.currentTimeMillis() / 1000) % TIME_STEP_SECONDS
        return (TIME_STEP_SECONDS - elapsed).toInt()
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

/**
 * Represents a TOTP account with a name/label and secret key.
 */
data class TotpAccount(
    val account: String,
    val secret: String,
    val issuer: String = "OAuth2Server"
)
