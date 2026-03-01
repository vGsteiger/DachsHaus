package com.dachshaus.auth.api.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class JwksController {
    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> {
        return mapOf("keys" to emptyList<Any>())
    }
}
