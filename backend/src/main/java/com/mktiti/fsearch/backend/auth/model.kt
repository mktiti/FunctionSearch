package com.mktiti.fsearch.backend.auth

enum class Role {
    USER, ADMIN
}

data class User(
        val username: String,
        val role: Role
)
