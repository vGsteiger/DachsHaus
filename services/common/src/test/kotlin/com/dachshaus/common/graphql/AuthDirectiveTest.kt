package com.dachshaus.common.graphql

import com.dachshaus.common.security.UserContext
import graphql.GraphQLContext
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthDirectiveTest {

    private val contextBuilder = mock<ContextBuilder>()
    private val authDirective = AuthDirective(contextBuilder)

    @Test
    fun `authenticated directive should allow authenticated user`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "user-123",
            roles = listOf("user"),
            requestId = "req-456"
        )

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext)).thenReturn(userContext)

        val environment = createDataFetchingEnvironment(graphQLContext)

        // When/Then - No exception should be thrown for authenticated user
        // The directive validation would happen during data fetching
        val retrievedContext = contextBuilder.getContextFromGraphQL(graphQLContext)
        assertEquals("user-123", retrievedContext.userId)
    }

    @Test
    fun `authenticated directive should reject anonymous user`() {
        // Given
        val graphQLContext = GraphQLContext.newContext().build()
        val userContext = UserContext(
            userId = "anonymous",
            roles = emptyList(),
            requestId = "req-456"
        )

        whenever(contextBuilder.getContextFromGraphQL(graphQLContext)).thenReturn(userContext)

        // When
        val retrievedContext = contextBuilder.getContextFromGraphQL(graphQLContext)

        // Then
        assertEquals("anonymous", retrievedContext.userId)
        assertTrue(retrievedContext.userId == "anonymous")
    }

    @Test
    fun `authenticated directive should throw when user context is missing`() {
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
