package com.dachshaus.common.graphql

/**
 * Exception thrown when GraphQL authorization fails.
 *
 * This exception is used by directive handlers to reject unauthorized requests.
 * The GraphQL framework will automatically convert this to a proper GraphQL error response.
 */
class UnauthorizedException(
    message: String
) : RuntimeException(message)
