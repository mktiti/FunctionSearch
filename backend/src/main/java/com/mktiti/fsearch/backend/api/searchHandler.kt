package com.mktiti.fsearch.backend.api

interface SearchHandler {

    fun typeHint(contextId: QueryCtxDto, namePart: String): List<TypeHint>

    fun syncQuery(contextId: QueryCtxDto, query: String): QueryResult

}