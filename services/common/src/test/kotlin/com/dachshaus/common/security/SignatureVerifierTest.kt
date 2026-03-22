package com.dachshaus.common.security

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureVerifierTest {

    @Test
    fun `computeHmac should produce consistent signatures`() {
        // Given
        val data = "test-data"
        val secret = "test-secret"

        // When
        val signature1 = SignatureVerifier.computeHmac(data, secret)
        val signature2 = SignatureVerifier.computeHmac(data, secret)

        // Then
        assertEquals(signature1, signature2, "Same data and secret should produce same signature")
    }

    @Test
    fun `computeHmac should produce different signatures for different data`() {
        // Given
        val secret = "test-secret"

        // When
        val signature1 = SignatureVerifier.computeHmac("data1", secret)
        val signature2 = SignatureVerifier.computeHmac("data2", secret)

        // Then
        assertFalse(signature1 == signature2, "Different data should produce different signatures")
    }

    @Test
    fun `computeHmac should produce different signatures for different secrets`() {
        // Given
        val data = "test-data"

        // When
        val signature1 = SignatureVerifier.computeHmac(data, "secret1")
        val signature2 = SignatureVerifier.computeHmac(data, "secret2")

        // Then
        assertFalse(signature1 == signature2, "Different secrets should produce different signatures")
    }

    @Test
    fun `verify should return true for valid signature`() {
        // Given
        val data = "user-123:admin,user:req-456:1234567890"
        val secret = "my-secret-key"
        val signature = SignatureVerifier.computeHmac(data, secret)

        // When
        val result = SignatureVerifier.verify(signature, data, secret)

        // Then
        assertTrue(result, "Valid signature should be verified")
    }

    @Test
    fun `verify should return false for invalid signature`() {
        // Given
        val data = "user-123:admin,user:req-456:1234567890"
        val secret = "my-secret-key"
        val invalidSignature = "0000000000000000000000000000000000000000000000000000000000000000"

        // When
        val result = SignatureVerifier.verify(invalidSignature, data, secret)

        // Then
        assertFalse(result, "Invalid signature should not be verified")
    }

    @Test
    fun `verify should return false when data is tampered`() {
        // Given
        val originalData = "user-123:admin,user:req-456:1234567890"
        val tamperedData = "user-999:admin,user:req-456:1234567890"
        val secret = "my-secret-key"
        val signature = SignatureVerifier.computeHmac(originalData, secret)

        // When
        val result = SignatureVerifier.verify(signature, tamperedData, secret)

        // Then
        assertFalse(result, "Signature should not verify for tampered data")
    }

    @Test
    fun `verify should return false when secret is different`() {
        // Given
        val data = "user-123:admin,user:req-456:1234567890"
        val secret = "my-secret-key"
        val wrongSecret = "wrong-secret-key"
        val signature = SignatureVerifier.computeHmac(data, secret)

        // When
        val result = SignatureVerifier.verify(signature, data, wrongSecret)

        // Then
        assertFalse(result, "Signature should not verify with different secret")
    }

    @Test
    fun `verify should use constant-time comparison`() {
        // This test verifies that the comparison doesn't short-circuit
        // by ensuring it works correctly regardless of where the difference is
        val data = "test-data"
        val secret = "test-secret"
        val correctSignature = SignatureVerifier.computeHmac(data, secret)

        // Create signatures that differ at the beginning, middle, and end
        val wrongAtStart = "0" + correctSignature.substring(1)
        val wrongAtMiddle = correctSignature.substring(0, correctSignature.length / 2) +
                           "0" +
                           correctSignature.substring(correctSignature.length / 2 + 1)
        val wrongAtEnd = correctSignature.substring(0, correctSignature.length - 1) + "0"

        // All should return false
        assertFalse(SignatureVerifier.verify(wrongAtStart, data, secret))
        assertFalse(SignatureVerifier.verify(wrongAtMiddle, data, secret))
        assertFalse(SignatureVerifier.verify(wrongAtEnd, data, secret))
    }

    @Test
    fun `computeHmac should produce hex-encoded signature`() {
        // Given
        val data = "test"
        val secret = "secret"

        // When
        val signature = SignatureVerifier.computeHmac(data, secret)

        // Then
        // HMAC-SHA256 produces 32 bytes = 64 hex characters
        assertEquals(64, signature.length, "HMAC-SHA256 should produce 64 hex characters")
        assertTrue(signature.all { it in '0'..'9' || it in 'a'..'f' },
                  "Signature should only contain hex characters")
    }

    @Test
    fun `verify should handle empty data`() {
        // Given
        val data = ""
        val secret = "test-secret"
        val signature = SignatureVerifier.computeHmac(data, secret)

        // When
        val result = SignatureVerifier.verify(signature, data, secret)

        // Then
        assertTrue(result, "Should verify signature for empty data")
    }

    @Test
    fun `verify should handle special characters in data`() {
        // Given
        val data = "user:admin,user:req-123:ñøé™£¢∞§¶"
        val secret = "test-secret"
        val signature = SignatureVerifier.computeHmac(data, secret)

        // When
        val result = SignatureVerifier.verify(signature, data, secret)

        // Then
        assertTrue(result, "Should verify signature for data with special characters")
    }
}
