package com.dachshaus.auth.domain.model

data class Credential(
    val id: String,
    val email: String,
    val passwordHash: String,
    val roles: List<String>
)
