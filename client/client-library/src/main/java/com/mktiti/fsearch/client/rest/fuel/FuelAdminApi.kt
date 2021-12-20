package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.AdminApi
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.dto.SearchStatistics

internal class FuelAdminApi(
        private val fuel: RequestFactory.Convenience
) : AdminApi {

    override fun searchStats(): ApiCallResult<SearchStatistics> {
        return fuel.getJson("statistics/search")
    }

}