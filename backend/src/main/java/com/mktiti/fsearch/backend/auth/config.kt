package com.mktiti.fsearch.backend.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.convert.DurationUnit
import org.springframework.validation.annotation.Validated
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.validation.constraints.NotBlank

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "auth")
data class AuthConfig(
        val jwt: JwtConfig,
        val admin: AdminAuthConfig,
        val testUsers: List<TestUserCredential>? = null
)

data class JwtConfig(
        @NotBlank val secret: String,
        @DurationUnit(ChronoUnit.DAYS) val expiry: Duration?
)

data class AdminAuthConfig(
        val enabled: Boolean,
        @NotBlank val password: String
)

data class TestUserCredential(
        @NotBlank val username: String,
        @NotBlank val password: String
)