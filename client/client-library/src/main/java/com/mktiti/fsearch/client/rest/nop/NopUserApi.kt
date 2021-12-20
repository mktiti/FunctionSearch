package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.UserApi
import com.mktiti.fsearch.dto.UserInfo

internal object NopUserApi : UserApi {

    override fun selfData(): ApiCallResult<UserInfo> = nopResult()
    
}