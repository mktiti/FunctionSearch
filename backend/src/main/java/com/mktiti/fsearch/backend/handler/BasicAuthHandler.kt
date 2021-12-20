package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.auth.AuthenticationService
import com.mktiti.fsearch.backend.auth.JwtService
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult
import com.mktiti.fsearch.rest.api.handler.AuthHandler

class BasicAuthHandler(
        private val authService: AuthenticationService,
        private val jwtService: JwtService
) : AuthHandler {

    override fun login(credentials: Credentials): LoginResult {
        val user = authService.checkCredentials(credentials.username, credentials.password) ?: return LoginResult.Invalid
        val jwt = jwtService.issueJwt(user)
        return LoginResult.Success(user.username, user.role.toDto(), jwt)
    }

}