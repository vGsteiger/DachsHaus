package com.dachshaus.common.graphql

import graphql.schema.DataFetcherFactories
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import org.springframework.stereotype.Component

/**
 * GraphQL directive handler for @authenticated directive.
 *
 * This directive ensures that only authenticated users (non-anonymous) can access
 * the annotated field or query. If the user ID is "anonymous", the request is rejected
 * with a 403 Forbidden error.
 *
 * Usage in schema:
 * ```graphql
 * type Query {
 *   me: User @authenticated
 * }
 * ```
 *
 * The directive checks the UserContext populated by the GatewaySignatureFilter.
 */
@Component
class AuthDirective(
    private val contextBuilder: ContextBuilder
) : SchemaDirectiveWiring {

    companion object {
        const val DIRECTIVE_NAME = "authenticated"
        private const val ANONYMOUS_USER_ID = "anonymous"
    }

    override fun onField(environment: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {
        val field = environment.element
        val parentTypeName = environment.fieldsContainer.name
        val coordinates = FieldCoordinates.coordinates(parentTypeName, field.name)

        val originalDataFetcher = environment.codeRegistry.getDataFetcher(
            coordinates,
            field
        )

        val authDataFetcher = DataFetcherFactories.wrapDataFetcher(
            originalDataFetcher
        ) { dataFetchingEnvironment, value ->
            // Extract UserContext from GraphQL context
            val graphQLContext = dataFetchingEnvironment.graphQlContext
            val userContext = try {
                contextBuilder.getContextFromGraphQL(graphQLContext)
            } catch (e: IllegalStateException) {
                throw UnauthorizedException("Authentication required: User context not found")
            }

            // Check if user is anonymous
            if (userContext.userId == ANONYMOUS_USER_ID) {
                throw UnauthorizedException("Authentication required: Anonymous access not allowed")
            }

            // User is authenticated, proceed with original data fetcher
            value
        }

        // Update the code registry with the wrapped data fetcher
        environment.codeRegistry.dataFetcher(
            coordinates,
            authDataFetcher
        )

        return field
    }
}
