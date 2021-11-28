package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.dto.HealthInfo
import com.mktiti.fsearch.dto.QueryRequestDto
import com.mktiti.fsearch.dto.QueryResult

internal class FuelSearchApi(
        private val fuel: RequestFactory.Convenience
) : SearchApi {

    override fun healthCheck(): ApiCallResult<HealthInfo> {
        return fuel.getJson("health_check")
    }

    override fun search(context: QueryRequestDto): ApiCallResult<QueryResult> {
        return fuel.postJson("search", context)
    }

}