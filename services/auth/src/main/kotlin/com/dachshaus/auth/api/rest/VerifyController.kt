package com.dachshaus.auth.api.rest

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class VerifyController {
    @PostMapping("/verify")
    fun verify(): Map<String, Any> {
        return mapOf("status" to "ok")
    }
}
