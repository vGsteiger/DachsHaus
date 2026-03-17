package com.dachshaus.common.graphql

import com.dachshaus.common.security.UserContext
import graphql.GraphQLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContextBuilderTest {

    private val contextBuilder = ContextBuilder()

    @Test
    fun `buildContext should extract UserContext from headers`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("X-User-Id", "user-123")
        request.addHeader("X-User-Roles", "user,premium")
        request.addHeader("X-Request-Id", "req-456")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        try {
            // When
            val userContext = contextBuilder.buildContext()

            // Then
            assertEquals("user-123", userContext.userId)
            assertEquals(listOf("user", "premium"), userContext.roles)
            assertEquals("req-456", userContext.requestId)
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }

    @Test
    fun `buildContext should handle anonymous user`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("X-User-Id", "anonymous")
        request.addHeader("X-User-Roles", "none")
        request.addHeader("X-Request-Id", "req-789")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        try {
            // When
            val userContext = contextBuilder.buildContext()

            // Then
            assertEquals("anonymous", userContext.userId)
            assertEquals(emptyList(), userContext.roles)
            assertEquals("req-789", userContext.requestId)
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }

    @Test
    fun `buildContext should throw when X-User-Id header is missing`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("X-User-Roles", "user")
        request.addHeader("X-Request-Id", "req-456")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        try {
            // When/Then
            val exception = assertThrows<IllegalStateException> {
                contextBuilder.buildContext()
            }
            assertEquals("Missing X-User-Id header", exception.message)
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }

    @Test
    fun `buildContext should throw when X-Request-Id header is missing`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("X-User-Id", "user-123")
        request.addHeader("X-User-Roles", "user")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        try {
            // When/Then
            val exception = assertThrows<IllegalStateException> {
                contextBuilder.buildContext()
            }
            assertEquals("Missing X-Request-Id header", exception.message)
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }

    @Test
    fun `setContextInGraphQL and getContextFromGraphQL should work together`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "user-123",
            roles = listOf("user", "admin"),
            requestId = "req-456"
        )

        // When
        contextBuilder.setContextInGraphQL(graphQLContext, userContext)
        val retrieved = contextBuilder.getContextFromGraphQL(graphQLContext)

        // Then
        assertNotNull(retrieved)
        assertEquals(userContext.userId, retrieved.userId)
        assertEquals(userContext.roles, retrieved.roles)
        assertEquals(userContext.requestId, retrieved.requestId)
    }

    @Test
    fun `getContextFromGraphQL should throw when context is missing`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            contextBuilder.getContextFromGraphQL(graphQLContext)
        }
        assertEquals("UserContext not found in GraphQL context", exception.message)
    }

    @Test
    fun `buildContext should handle empty roles`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("X-User-Id", "user-123")
        request.addHeader("X-User-Roles", "")
        request.addHeader("X-Request-Id", "req-456")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        try {
            // When
            val userContext = contextBuilder.buildContext()

            // Then
            assertEquals(emptyList(), userContext.roles)
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
    }
}
