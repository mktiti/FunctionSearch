package com.mktiti.fsearch.frontend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val dtoJson = Json {
    classDiscriminator = "type"
}

@Serializable
data class ArtifactIdDto(
        val group: String,
        val name: String,
        val version: String
)

@Serializable
data class QueryCtxDto(
        val artifacts: List<ArtifactIdDto>
)

@Serializable
data class HintRequestDto(
        val context: QueryCtxDto,
        val name: String
)

@Serializable
data class QueryRequestDto(
        val context: QueryCtxDto,
        val query: String
)

@Serializable
data class TypeHint(
        val file: String,
        val typeParamCount: Int
)

@Serializable
data class FunDocDto(
        val shortInfo: String?,
        val details: String?
)

// TODO Fitting result
@Serializable
data class QueryFitResult(
        val file: String,
        val funName: String,
        val static: Boolean,
        val header: String,
        val doc: FunDocDto
)

@Serializable
sealed class QueryResult {

    abstract val query: String


    @Serializable
    sealed class Error : QueryResult() {

        abstract override val query: String
        abstract val message: String

        @Serializable
        @SerialName("QueryResult\$Error\$InternalError")
        class InternalError(
                override val query: String,
                override val message: String
        ) : Error()

        @Serializable
        @SerialName("QueryResult\$Error\$Query")
        class Query(
                override val query: String,
                override val message: String
        ) : Error()

    }

    @Serializable
    @SerialName("QueryResult\$Success")
    class Success(
            override val query: String,
            val results: List<QueryFitResult>
    ) : QueryResult()

}
