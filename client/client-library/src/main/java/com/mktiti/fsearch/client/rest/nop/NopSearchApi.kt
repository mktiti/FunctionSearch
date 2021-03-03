package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.SearchApi
import com.mktiti.fsearch.dto.MessageDto
import com.mktiti.fsearch.dto.QueryRequestDto
import com.mktiti.fsearch.dto.QueryResult

internal object NopSearchApi : SearchApi {

    override fun healthCheck(): ApiCallResult<MessageDto> = nopResult()

    override fun search(context: QueryRequestDto): ApiCallResult<QueryResult> = nopResult()

}