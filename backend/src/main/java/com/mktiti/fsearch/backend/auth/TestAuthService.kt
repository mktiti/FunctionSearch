package com.mktiti.fsearch.backend.auth

import java.time.LocalDateTime

class TestAuthService(
        private val authConfig: AuthConfig
) : AuthenticationService {

    companion object {
        private val adminUser = User("admin", Role.ADMIN)
    }

    private val userPasswordMap: Map<String, String> = authConfig.testUsers?.map {
        it.username to it.password
    }?.toMap() ?: emptyMap()

    override fun checkCredentials(username: String, password: String): User? {
        return if (username == adminUser.username) {
            if (password == authConfig.admin.password && authConfig.admin.enabled) adminUser else null
        } else {
            if (password == userPasswordMap[username]) User(username, Role.USER) else null
        }
    }

    override fun issueJwt(user: User, expiry: LocalDateTime): String {
        return "TODO-ISSUE-JWT"
    }

}