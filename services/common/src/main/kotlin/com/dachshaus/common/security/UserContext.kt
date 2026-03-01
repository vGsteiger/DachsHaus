package com.dachshaus.common.security

data class UserContext(
    val userId: String,
    val roles: List<String>,
    val requestId: String
)
