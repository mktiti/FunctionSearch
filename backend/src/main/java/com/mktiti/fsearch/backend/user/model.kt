package com.mktiti.fsearch.backend.user

import com.mktiti.fsearch.rest.api.User
import java.time.Instant

enum class Level {
    NORMAL, PREMIUM
}

data class UserInfo(
        val user: User,
        val registerDate: Instant,
        val level: Level
)