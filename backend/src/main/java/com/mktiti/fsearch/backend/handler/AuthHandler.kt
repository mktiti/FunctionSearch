package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult

interface AuthHandler {

    object Nop : AuthHandler {
        override fun login(credentials: Credentials): LoginResult = LoginResult.Invalid
    }

    fun login(credentials: Credentials): LoginResult

}
