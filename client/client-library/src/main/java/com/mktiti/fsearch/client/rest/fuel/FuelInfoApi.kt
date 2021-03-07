package com.mktiti.fsearch.client.rest.fuel

import com.github.kittinunf.fuel.core.RequestFactory
import com.mktiti.fsearch.client.rest.ApiCallResult
import com.mktiti.fsearch.client.rest.InfoApi
import com.mktiti.fsearch.dto.*

internal class FuelInfoApi(
        private val fuel: RequestFactory.Convenience
) : InfoApi {

    override fun types(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<TypeInfoDto>> {
        return fuel.postJson("types", ContextInfoQueryParam(context, namePartOpt))
    }

    override fun functions(context: QueryCtxDto, namePartOpt: String?): ApiCallResult<ResultList<FunId>> {
        return fuel.postJson("functions", ContextInfoQueryParam(context, namePartOpt))
    }


}