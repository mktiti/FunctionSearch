package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.dto.*

interface SearchHandler {

    object Nop : SearchHandler {
        override fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint> = ResultList.empty()

        override fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus = ContextLoadStatus.LOADING

        override fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult = QueryResult.Error.InternalError(
            query = query, message = "NOP handler, not implemented"
        )
    }

    fun typeHint(contextId: QueryCtxDto, namePart: String): ResultList<TypeHint>

    fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus

    fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult

}
