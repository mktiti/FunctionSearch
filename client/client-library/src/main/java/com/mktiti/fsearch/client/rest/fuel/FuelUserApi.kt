package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.UserApi
import com.mktiti.fsearch.dto.UserInfo

internal class FuelUserApi(
        private val fuel: RequestFactory.Convenience
) : UserApi {

    override fun selfData(): ApiCallResult<UserInfo> {
        return fuel.getJson("user/self")
    }

}