package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.backend.ContextId
import com.mktiti.fsearch.modules.ArtifactId

data class ArtifactIdDto(
        val group: String,
        val name: String,
        val version: String
) {

    fun toId() = ArtifactId(group.split('.'), name, version)

}

data class QueryCtxDto(
        val artifacts: List<ArtifactIdDto>
) {

    fun toId() = ContextId(artifacts.map { it.toId() }.toSet())

}

data class HintRequestDto(
        val context: QueryCtxDto,
        val name: String
)

data class QueryRequestDto(
        val context: QueryCtxDto,
        val query: String
)

data class TypeHint(
        val file: String,
        val typeParamCount: Int
)

data class FunDocDto(
        val shortInfo: String?,
        val details: String?
)

// TODO Fitting result
data class QueryFitResult(
        val file: String,
        val funName: String,
        val isStatic: Boolean,
        val header: String,
        val doc: FunDocDto
)

sealed class QueryResult(
        val query: String
) {

    sealed class Error(
            query: String,
            val message: String
    ) : QueryResult(query) {

        class InternalError(query: String, message: String) : Error(query, message)

        class Query(query: String, message: String) : Error(query, message)

    }

    class Success(
            query: String,
            val results: List<QueryFitResult>
    ) : QueryResult(query)

}