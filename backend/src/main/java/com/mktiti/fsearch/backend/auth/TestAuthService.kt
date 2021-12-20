package com.mktiti.fsearch.backend.auth

import com.mktiti.fsearch.rest.api.Role
import com.mktiti.fsearch.rest.api.User

class TestAuthService(
        private val authConfig: AuthConfig
) : AuthenticationService {

    companion object {
        private val adminUser = User("admin", Role.ADMIN)
    }

    private val userPasswordMap: Map<String, String> = authConfig.testUsers?.associate {
        it.username to it.password
    } ?: emptyMap()

    override fun checkCredentials(username: String, password: String): User? {
        return if (username == adminUser.username) {
            if (password == authConfig.admin.password && authConfig.admin.enabled) adminUser else null
        } else {
            if (password == userPasswordMap[username]) User(username, Role.USER) else null
        }
    }

}