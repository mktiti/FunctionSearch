package com.mktiti.fsearch.backend.handler.basic

import com.mktiti.fsearch.backend.auth.AuthenticationService
import com.mktiti.fsearch.backend.handler.AuthHandler
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import java.time.LocalDateTime

class BasicAuthHandler(
        private val authService: AuthenticationService
) : AuthHandler {

    override fun login(credentials: Credentials): LoginResult {
        val user = authService.checkCredentials(credentials.username, credentials.password) ?: return LoginResult.Invalid
        val jwt = authService.issueJwt(user, LocalDateTime.MAX)
        return LoginResult.Success(user.username, user.role.toDto(), jwt)
    }

}