package com.dachshaus.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.test.assertEquals

class GatewaySignatureFilterTest {

    private lateinit var filter: GatewaySignatureFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var chain: FilterChain

    private val testSecret = "test-secret-key"

    @BeforeEach
    fun setup() {
        filter = GatewaySignatureFilter(testSecret, 30)
        request = mock()
        response = mock()
        chain = mock()
    }

    @Test
    fun `filter should reject request without signature header`() {
        // Given
        whenever(request.getHeader("X-Gateway-Signature")).thenReturn(null)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Missing gateway signature")
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with empty signature header`() {
        // Given
        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("")

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Missing gateway signature")
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with invalid signature format`() {
        // Given
        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("invalid-format")

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature format")
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with non-numeric timestamp`() {
        // Given
        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("not-a-number.abc123")

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid timestamp format")
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with expired timestamp`() {
        // Given
        val expiredTimestamp = Instant.now().epochSecond - 60 // 60 seconds ago (> 30s threshold)
        val userId = "user-123"
        val userRoles = "admin,user"
        val requestId = "req-456"
        val payload = "$userId:$userRoles:$requestId:$expiredTimestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$expiredTimestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(
            eq(HttpServletResponse.SC_FORBIDDEN),
            argThat { this.startsWith("Timestamp expired") }
        )
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with future timestamp beyond threshold`() {
        // Given
        val futureTimestamp = Instant.now().epochSecond + 60 // 60 seconds in future (> 30s threshold)
        val userId = "user-123"
        val userRoles = "admin,user"
        val requestId = "req-456"
        val payload = "$userId:$userRoles:$requestId:$futureTimestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$futureTimestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(
            eq(HttpServletResponse.SC_FORBIDDEN),
            argThat { this.startsWith("Timestamp expired") }
        )
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should reject request with invalid signature`() {
        // Given
        val timestamp = Instant.now().epochSecond
        val userId = "user-123"
        val userRoles = "admin,user"
        val requestId = "req-456"
        val invalidSignature = "0000000000000000000000000000000000000000000000000000000000000000"

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$invalidSignature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
        verify(chain, never()).doFilter(any(), any())
    }

    @Test
    fun `filter should accept request with valid signature and recent timestamp`() {
        // Given
        val timestamp = Instant.now().epochSecond
        val userId = "user-123"
        val userRoles = "admin,user"
        val requestId = "req-456"
        val payload = "$userId:$userRoles:$requestId:$timestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response, never()).sendError(any(), any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `filter should accept request with timestamp within 30 second threshold`() {
        // Given
        val timestamp = Instant.now().epochSecond - 25 // 25 seconds ago (within 30s threshold)
        val userId = "user-123"
        val userRoles = "user"
        val requestId = "req-789"
        val payload = "$userId:$userRoles:$requestId:$timestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response, never()).sendError(any(), any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `filter should handle anonymous user with valid signature`() {
        // Given
        val timestamp = Instant.now().epochSecond
        val userId = "anonymous"
        val userRoles = "none"
        val requestId = "req-anon"
        val payload = "$userId:$userRoles:$requestId:$timestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(userId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response, never()).sendError(any(), any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `filter should use default values when headers are missing`() {
        // Given
        val timestamp = Instant.now().epochSecond
        // Headers will be null, so defaults will be used: userId=anonymous, userRoles=none, requestId=""
        val payload = "anonymous:none::$timestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(null)
        whenever(request.getHeader("X-User-Roles")).thenReturn(null)
        whenever(request.getHeader("X-Request-Id")).thenReturn(null)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response, never()).sendError(any(), any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `filter should reject when signature is valid but headers are tampered`() {
        // Given
        val timestamp = Instant.now().epochSecond
        val originalUserId = "user-123"
        val tamperedUserId = "user-999"
        val userRoles = "admin,user"
        val requestId = "req-456"

        // Signature is for original user ID
        val payload = "$originalUserId:$userRoles:$requestId:$timestamp"
        val signature = SignatureVerifier.computeHmac(payload, testSecret)

        // But we send tampered user ID
        whenever(request.getHeader("X-Gateway-Signature")).thenReturn("$timestamp.$signature")
        whenever(request.getHeader("X-User-Id")).thenReturn(tamperedUserId)
        whenever(request.getHeader("X-User-Roles")).thenReturn(userRoles)
        whenever(request.getHeader("X-Request-Id")).thenReturn(requestId)

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature")
        verify(chain, never()).doFilter(any(), any())
    }
}
