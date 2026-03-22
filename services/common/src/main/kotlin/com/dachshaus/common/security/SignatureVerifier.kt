package com.dachshaus.common.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies HMAC-SHA256 signatures from the gateway.
 *
 * Uses constant-time comparison to prevent timing attacks.
 */
object SignatureVerifier {
    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Verifies an HMAC-SHA256 signature.
     *
     * @param signature The signature to verify (hex-encoded)
     * @param data The data that was signed
     * @param secret The shared secret key
     * @return true if the signature is valid, false otherwise
     */
    fun verify(signature: String, data: String, secret: String): Boolean {
        val expectedSignature = computeHmac(data, secret)
        return constantTimeEquals(signature, expectedSignature)
    }

    /**
     * Computes HMAC-SHA256 of the given data.
     *
     * @param data The data to sign
     * @param secret The shared secret key
     * @return The HMAC signature as a hex-encoded string
     */
    fun computeHmac(data: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM)
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * This method always compares all characters, even if an early mismatch is found,
     * to ensure the comparison time doesn't leak information about where the mismatch occurred.
     *
     * @param a First string to compare
     * @param b Second string to compare
     * @return true if strings are equal, false otherwise
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) {
            return false
        }

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }

        return result == 0
    }
}
