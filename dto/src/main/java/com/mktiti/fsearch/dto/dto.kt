package com.mktiti.fsearch.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@SuppressWarnings("unused")

enum class ContextLoadStatus {
    LOADING, LOADED, ERROR
}

data class MessageDto(val message: String)

data class ArtifactIdDto(
        val group: String,
        val name: String,
        val version: String
) {

    override fun toString(): String {
        return "$group:$name$version"
    }

}

data class QueryCtxDto(
        val artifacts: List<ArtifactIdDto>
)

data class TypeDto(
        val packageName: String,
        val simpleName: String
)

data class TypeInfoDto(
        val type: TypeDto,
        val typeParamCount: Int
)

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
}

data class ContextInfoQueryParam(
        val context: QueryCtxDto,
        val namePart: String?
)

data class FunId(
        val type: TypeDto,
        val name: String,
        val signature: String,
        val relation: FunRelationDto
)

// TODO Fitting result, rename to Function?
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
