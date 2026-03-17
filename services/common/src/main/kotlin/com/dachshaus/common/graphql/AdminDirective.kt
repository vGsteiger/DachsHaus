package com.dachshaus.common.graphql

import graphql.schema.DataFetcherFactories
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import org.springframework.stereotype.Component

/**
 * GraphQL directive handler for @admin directive.
 *
 * This directive ensures that only users with the "admin" role can access
 * the annotated field, query, or mutation. If the user does not have the admin role,
 * the request is rejected with a 403 Forbidden error.
 *
 * Usage in schema:
 * ```graphql
 * type Mutation {
 *   deleteUser(id: ID!): Boolean @admin
 * }
 * ```
 *
 * The directive checks the UserContext populated by the GatewaySignatureFilter
 * and verifies that "admin" is present in the user's roles list.
 */
@Component
class AdminDirective(
    private val contextBuilder: ContextBuilder
) : SchemaDirectiveWiring {

    companion object {
        const val DIRECTIVE_NAME = "admin"
        private const val ADMIN_ROLE = "admin"
    }

    override fun onField(environment: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {
        val field = environment.element
        val originalDataFetcher = environment.codeRegistry.getDataFetcher(
            environment.fieldsContainer,
            field
        )

        val adminDataFetcher = DataFetcherFactories.wrapDataFetcher(
            originalDataFetcher
        ) { dataFetchingEnvironment, value ->
            // Extract UserContext from GraphQL context
            val graphQLContext = dataFetchingEnvironment.graphQlContext
            val userContext = try {
                contextBuilder.getContextFromGraphQL(graphQLContext)
            } catch (e: IllegalStateException) {
                throw UnauthorizedException("Authorization required: User context not found")
            }

            // Check if user has admin role
            if (!userContext.roles.contains(ADMIN_ROLE)) {
                throw UnauthorizedException("Authorization required: Admin role required")
            }

            // User is admin, proceed with original data fetcher
            value
        }

        // Update the code registry with the wrapped data fetcher
        environment.codeRegistry.dataFetcher(
            environment.fieldsContainer,
            field,
            adminDataFetcher
        )

        return field
    }
}
