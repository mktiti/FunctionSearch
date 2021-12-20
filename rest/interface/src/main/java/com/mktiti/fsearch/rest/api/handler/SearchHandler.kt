package com.mktiti.fsearch.rest.api.handler

import com.mktiti.fsearch.dto.*

interface SearchHandler {

    object Nop : SearchHandler {
        override fun healthCheck(): HealthInfo = HealthInfo("NOP", "NOP", true)

        override fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint> = ResultList.empty()

        override fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus = ContextLoadStatus.LOADING

        override fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult = QueryResult.Error.InternalError(
            query = query, message = "NOP handler, not implemented"
        )
    }

    fun healthCheck(): HealthInfo

    fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint>

    fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus

    fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult

}
