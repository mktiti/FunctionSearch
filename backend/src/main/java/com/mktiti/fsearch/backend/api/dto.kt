package com.mktiti.fsearch.backend.api

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.mktiti.fsearch.backend.ContextId
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.modules.ArtifactId
import io.swagger.v3.oas.annotations.media.Schema

enum class ContextLoadStatus {
    LOADING, LOADED, ERROR
}

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

enum class FunRelationDto {
    STATIC, CONSTRUCTOR, INSTANCE;

    companion object {
        fun fromModel(rel: FunInstanceRelation) = when (rel) {
            FunInstanceRelation.INSTANCE -> INSTANCE
            FunInstanceRelation.STATIC -> STATIC
            FunInstanceRelation.CONSTRUCTOR -> CONSTRUCTOR
        }
    }
}

// TODO Fitting result
data class QueryFitResult(
        val file: String,
        val funName: String,
        val relation: FunRelationDto,
        val header: String,
        val doc: FunDocDto
)

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@Schema(name = "QueryResult", oneOf = [
    QueryResult.Error.InternalError::class, QueryResult.Error.Query::class, QueryResult.Success::class
])
sealed class QueryResult {

    abstract val query: String

    sealed class Error : QueryResult() {

        abstract override val query: String
        abstract val message: String

        @Schema(name = "QueryResultInternalError")
        class InternalError(
                override val query: String,
                override val message: String
        ) : Error()

        @Schema(name = "QueryResultQueryError")
        class Query(
                override val query: String,
                override val message: String
        ) : Error()

    }

    @Schema(name = "QueryResultSuccess")
    class Success(
            override val query: String,
            val results: List<QueryFitResult>
    ) : QueryResult()

}
