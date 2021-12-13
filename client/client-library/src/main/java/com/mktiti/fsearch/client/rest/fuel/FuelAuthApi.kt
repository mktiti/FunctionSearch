package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.AuthApi
import com.mktiti.fsearch.dto.Credentials
import com.mktiti.fsearch.dto.LoginResult

internal class FuelAuthApi(
        private val fuel: RequestFactory.Convenience
) : AuthApi {

    override fun login(credentials: Credentials): ApiCallResult<LoginResult> {
        return fuel.postJson("login", credentials)
    }

}