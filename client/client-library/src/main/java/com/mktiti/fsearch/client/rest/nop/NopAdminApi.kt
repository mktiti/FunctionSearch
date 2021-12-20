package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.AdminApi
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.dto.SearchStatistics

internal object NopAdminApi : AdminApi {

    override fun searchStats(): ApiCallResult<SearchStatistics> = nopResult()
    
}