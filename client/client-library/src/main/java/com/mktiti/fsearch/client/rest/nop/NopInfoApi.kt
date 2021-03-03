package com.mktiti.fsearch.client.rest.nop

import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeInfoDto

internal object NopInfoApi : InfoApi {
    
    override fun types(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<Collection<TypeInfoDto>> = nopResult()

    override fun functions(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<Collection<FunId>> = nopResult()
    
}