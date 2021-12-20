package com.mktiti.fsearch.dto

enum class UserLevel {
    NORMAL, PREMIUM
}

// Mainly for testing
data class UserInfo(
        val registerDate: String, // ISO-8601
        val level: UserLevel,
        val savedContexts: List<QueryCtxDto>
)