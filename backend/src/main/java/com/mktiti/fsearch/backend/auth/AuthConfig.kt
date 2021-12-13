package com.mktiti.fsearch.backend.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "auth")
data class AuthConfig(
        val admin: AdminAuthConfig,
        val testUsers: List<TestUserCredential>? = null
)

data class AdminAuthConfig(
        val enabled: Boolean,
        val password: String
)

data class TestUserCredential(
        val username: String,
        val password: String
)