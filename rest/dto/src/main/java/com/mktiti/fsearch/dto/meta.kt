package com.mktiti.fsearch.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

data class HealthInfo(
        val version: String,
        val buildTimestamp: String,
        val ok: Boolean
)

data class Credentials(
        val username: String,
        val password: String
)

enum class Role {
    USER, ADMIN
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@Schema(name = "LoginResult", oneOf = [
    LoginResult.Invalid::class, LoginResult.Success::class
])
sealed interface LoginResult {

    @Schema(name = "InvalidCredentials")
    object Invalid : LoginResult

    @Schema(name = "LoginSuccess")
    class Success(
            val username: String,
            val role: Role,
            val jwt: String
    ) : LoginResult
}