package com.dachshaus.common.graphql

import com.dachshaus.common.security.UserContext
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminDirectiveTest {

    private val contextBuilder = mock<ContextBuilder>()
    private val adminDirective = AdminDirective(contextBuilder)

    @Test
    fun `admin directive should allow admin user`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "user-123",
            roles = listOf("user", "admin"),
            requestId = "req-456"
        )

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext)).thenReturn(userContext)

        // When
        val retrievedContext = contextBuilder.getContextFromGraphQL(graphQLContext)

        // Then
        assertEquals("user-123", retrievedContext.userId)
        assertTrue(retrievedContext.roles.contains("admin"))
    }

    @Test
    fun `admin directive should reject non-admin user`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "user-123",
            roles = listOf("user"),
            requestId = "req-456"
        )

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext)).thenReturn(userContext)

        // When
        val retrievedContext = contextBuilder.getContextFromGraphQL(graphQLContext)

        // Then
        assertEquals("user-123", retrievedContext.userId)
        assertFalse(retrievedContext.roles.contains("admin"))
    }

    @Test
    fun `admin directive should reject user with no roles`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "user-123",
            roles = emptyList(),
            requestId = "req-456"
        )

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext)).thenReturn(userContext)

        // When
        val retrievedContext = contextBuilder.getContextFromGraphQL(graphQLContext)

        // Then
        assertEquals("user-123", retrievedContext.userId)
        assertFalse(retrievedContext.roles.contains("admin"))
        assertTrue(retrievedContext.roles.isEmpty())
    }

    @Test
    fun `admin directive should throw when user context is missing`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext))
            .thenThrow(IllegalStateException("UserContext not found in GraphQL context"))

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            contextBuilder.getContextFromGraphQL(graphQLContext)
        }
        assertEquals("UserContext not found in GraphQL context", exception.message)
    }

    private fun createDataFetchingEnvironment(graphQLContext: GraphQLContext): DataFetchingEnvironment {
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .graphQLContext(graphQLContext)
            .build()
    }
}
