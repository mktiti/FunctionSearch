package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.dto.ContextInfoQueryParam
import com.mktiti.fsearch.dto.FunId
import com.mktiti.fsearch.dto.QueryCtxDto
import com.mktiti.fsearch.dto.TypeInfoDto

internal class FuelInfoApi(
        private val fuel: RequestFactory.Convenience
) : InfoApi {

    override fun types(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<Collection<TypeInfoDto>> {
        return fuel.postJson("types", ContextInfoQueryParam(context, namePartOpt))
    }

    override fun functions(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<Collection<FunId>> {
        return fuel.postJson("functions", ContextInfoQueryParam(context, namePartOpt))
    }


}