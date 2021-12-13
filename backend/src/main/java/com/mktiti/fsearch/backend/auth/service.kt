package com.mktiti.fsearch.backend.auth

import java.time.LocalDateTime

interface AuthenticationService {

    object Nop : AuthenticationService {
        override fun checkCredentials(username: String, password: String): Nothing? = null
        override fun issueJwt(user: User, expiry: LocalDateTime): String = ""
    }

    fun checkCredentials(username: String, password: String): User?

    fun issueJwt(user: User, expiry: LocalDateTime): String

}
