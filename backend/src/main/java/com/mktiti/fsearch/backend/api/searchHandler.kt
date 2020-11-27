package com.mktiti.fsearch.backend.api

interface SearchHandler {

    fun typeHint(contextId: QueryCtxDto, namePart: String): List<TypeHint>

    fun preloadContext(contextId: QueryCtxDto): ContextLoadStatus

    fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult

}