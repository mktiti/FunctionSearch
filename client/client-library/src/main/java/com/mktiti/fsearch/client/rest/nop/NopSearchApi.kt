package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.dto.HealthInfo
import com.mktiti.fsearch.dto.QueryRequestDto
import com.mktiti.fsearch.dto.QueryResult

internal object NopSearchApi : SearchApi {

    override fun healthCheck(): ApiCallResult<HealthInfo> = nopResult()

    override fun search(context: QueryRequestDto): ApiCallResult<QueryResult> = nopResult()

}