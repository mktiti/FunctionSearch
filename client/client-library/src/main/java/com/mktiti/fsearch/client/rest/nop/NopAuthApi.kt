package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.AuthApi
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult

internal object NopAuthApi : AuthApi {

    override fun login(credentials: Credentials): ApiCallResult<LoginResult> = nopResult()
    
}