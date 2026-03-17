package com.dachshaus.common.graphql

import com.dachshaus.common.security.UserContext
import graphql.GraphQLContext
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Builds UserContext from HTTP headers set by the Gateway's HMAC signature filter.
 *
 * The GatewaySignatureFilter verifies the signature and sets these headers:
 * - X-User-Id: UUID or "anonymous"
 * - X-User-Roles: comma-separated roles or "none"
 * - X-Request-Id: UUID
 *
 * This context builder makes the UserContext available to GraphQL resolvers and directives.
 */
@Component
class ContextBuilder {

    companion object {
        const val USER_CONTEXT_KEY = "userContext"
        private const val HEADER_USER_ID = "X-User-Id"
        private const val HEADER_USER_ROLES = "X-User-Roles"
        private const val HEADER_REQUEST_ID = "X-Request-Id"
    }

    /**
     * Extracts UserContext from the current HTTP request headers.
     *
     * This method is called during GraphQL request processing to populate
     * the GraphQL context with authentication information.
     *
     * @return UserContext with user ID, roles, and request ID
     * @throws IllegalStateException if required headers are missing
     */
    fun buildContext(): UserContext {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw IllegalStateException("No request context available")

        val request = requestAttributes.request

        val userId = request.getHeader(HEADER_USER_ID)
            ?: throw IllegalStateException("Missing $HEADER_USER_ID header")

        val rolesHeader = request.getHeader(HEADER_USER_ROLES) ?: "none"
        val roles = if (rolesHeader == "none") {
            emptyList()
        } else {
            rolesHeader.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val requestId = request.getHeader(HEADER_REQUEST_ID)
            ?: throw IllegalStateException("Missing $HEADER_REQUEST_ID header")

        return UserContext(
            userId = userId,
            roles = roles,
            requestId = requestId
        )
    }

    /**
     * Stores UserContext in the GraphQL context.
     *
     * @param graphQLContext The GraphQL context to populate
     * @param userContext The UserContext to store
     */
    fun setContextInGraphQL(graphQLContext: GraphQLContext, userContext: UserContext) {
        graphQLContext.put(USER_CONTEXT_KEY, userContext)
    }

    /**
     * Retrieves UserContext from the GraphQL context.
     *
     * @param graphQLContext The GraphQL context
     * @return UserContext if present
     * @throws IllegalStateException if UserContext is not in the context
     */
    fun getContextFromGraphQL(graphQLContext: GraphQLContext): UserContext {
        return graphQLContext.get<UserContext>(USER_CONTEXT_KEY)
            ?: throw IllegalStateException("UserContext not found in GraphQL context")
    }
}
