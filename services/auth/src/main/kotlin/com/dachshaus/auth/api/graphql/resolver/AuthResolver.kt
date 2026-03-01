package com.dachshaus.auth.api.graphql.resolver

import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class AuthResolver {
    @MutationMapping
    fun login(email: String, password: String): Map<String, Any> {
        return mapOf("token" to "")
    }
}
