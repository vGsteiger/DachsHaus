package com.dachshaus.auth.api.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/healthz")
    fun health(): Map<String, String> {
        return mapOf("status" to "UP")
    }
}
