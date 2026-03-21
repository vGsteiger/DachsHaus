package com.dachshaus.common.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.math.abs

/**
 * Servlet filter that verifies HMAC-SHA256 signatures from the gateway.
 *
 * This filter validates that requests to GraphQL subgraphs are properly signed by the gateway
 * using a shared secret. It performs the following checks:
 * 1. Ensures the X-Gateway-Signature header is present
 * 2. Validates the timestamp is within ±30 seconds of current time
 * 3. Verifies the HMAC-SHA256 signature using constant-time comparison
 *
 * Header format: X-Gateway-Signature: {timestamp}.{hmac}
 * HMAC payload: {x-user-id}:{x-user-roles}:{x-request-id}:{timestamp}
 */
@Component
class GatewaySignatureFilter(
    @Value("\${gateway.signature.secret:changeme}") private val secret: String,
    @Value("\${gateway.signature.maxSkewSeconds:30}") private val maxSkewSeconds: Long = 30
) : Filter {

    companion object {
        private const val SIGNATURE_HEADER = "X-Gateway-Signature"
        private const val USER_ID_HEADER = "X-User-Id"
        private const val USER_ROLES_HEADER = "X-User-Roles"
        private const val REQUEST_ID_HEADER = "X-Request-Id"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // Extract signature header
        val signatureHeader = httpRequest.getHeader(SIGNATURE_HEADER)
        if (signatureHeader.isNullOrBlank()) {
            httpResponse.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Missing gateway signature"
            )
            return
        }

        // Parse signature header: {timestamp}.{hmac}
        val parts = signatureHeader.split(".")
        if (parts.size != 2) {
            httpResponse.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid signature format"
            )
            return
        }

        val timestampStr = parts[0]
        val signature = parts[1]

        // Validate timestamp
        val timestamp = try {
            timestampStr.toLong()
        } catch (e: NumberFormatException) {
            httpResponse.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid timestamp format"
            )
            return
        }

        val now = Instant.now().epochSecond
        val skew = abs(now - timestamp)
        if (skew > maxSkewSeconds) {
            httpResponse.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Timestamp expired (skew: ${skew}s)"
            )
            return
        }

        // Extract required headers for signature validation
        val userId = httpRequest.getHeader(USER_ID_HEADER) ?: "anonymous"
        val userRoles = httpRequest.getHeader(USER_ROLES_HEADER) ?: "none"
        val requestId = httpRequest.getHeader(REQUEST_ID_HEADER) ?: ""

        // Construct the payload that was signed
        val payload = "$userId:$userRoles:$requestId:$timestamp"

        // Verify signature using constant-time comparison
        if (!SignatureVerifier.verify(signature, payload, secret)) {
            httpResponse.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Invalid signature"
            )
            return
        }

        // Signature is valid, proceed with the request
        chain.doFilter(request, response)
    }
}
