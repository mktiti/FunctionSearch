package com.mktiti.fsearch.grpc.converter

import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.grpc.Artifact
import com.mktiti.fsearch.grpc.Common
import com.mktiti.fsearch.grpc.Info
import com.mktiti.fsearch.grpc.Search

private fun Common.Type.toDto() = TypeDto(
        packageName = packageName,
        simpleName = simpleName
)

private fun Info.TypeInfo.toDto() = TypeInfoDto(
        type = type.toDto(),
        typeParamCount = typeParamCount
)

private fun Info.FunInfo.toDto() = FunId(
        type = type.toDto(),
        name = name,
        signature = signature,
        relation = when (relation as Common.FunRelation) {
            Common.FunRelation.STATIC -> FunRelationDto.STATIC
            Common.FunRelation.CONSTRUCTOR -> FunRelationDto.CONSTRUCTOR
            Common.FunRelation.INSTANCE -> FunRelationDto.INSTANCE
        }
)

private fun Search.QueryFitResult.toDto() = QueryFitResult(
        file = file,
        funName = funName,
        relation = when (relation) {
            Common.FunRelation.STATIC -> FunRelationDto.STATIC
            Common.FunRelation.CONSTRUCTOR -> FunRelationDto.CONSTRUCTOR
            Common.FunRelation.INSTANCE, null -> FunRelationDto.INSTANCE
        },
        header = header,
        doc = FunDocDto(
                shortInfo = doc.shortInfo,
                details = doc.details
        )
)

fun Search.QueryResult.toDto(): QueryResult {
    return if (success != null) {
        val result = ResultList(
                results = success.resultsList.map { it.toDto() },
                trimmed = success.trimmed
        )
        QueryResult.Success(query, result)
    } else {
        when (error.type) {
            Search.Error.ErrorType.QUERY -> QueryResult.Error.Query(query, error.message)
            Search.Error.ErrorType.INTERNAL -> QueryResult.Error.InternalError(query, error.message)
            null -> QueryResult.Error.Query(query, "Unknown error type")
        }
    }
}

fun Info.TypeInfoResult.toDto() = ResultList(
        trimmed = trimmed,
        results = resultsList.map { it.toDto() }
)

fun Info.FunInfoResult.toDto() = ResultList(
        trimmed = trimmed,
        results = resultsList.map { it.toDto() }
)

fun Common.ArtifactId.toDto() = ArtifactIdDto(
        group = group,
        name = name,
        version = version
)

fun Common.QueryContext.toDto() = QueryCtxDto(
        artifacts = artifactsIdsList.map { it.toDto() },
        imports = importsList.map { it.toDto() }
)

fun Artifact.ArtifactListResult.toDto() = ResultList(
        trimmed = trimmed,
        results = resultsList.map { it.toDto() }
)

fun Artifact.ArtifactGetResult.toDto(): ArtifactIdDto? = result?.toDto()

sealed class FilterMessageDto {
    object AllFilter : FilterMessageDto()
    data class GroupFilter(val group: String) : FilterMessageDto()
    data class NameFilter(val group: String, val name: String) : FilterMessageDto()
}

fun Artifact.ArtifactFilterMessage.toDto() = when {
    group != null && name != null -> FilterMessageDto.NameFilter(group, name)
    group != null -> FilterMessageDto.GroupFilter(group)
    else -> FilterMessageDto.AllFilter
}

fun Artifact.ArtifactSelectMessage.toDto() = ArtifactIdDto(
        group = group,
        name = name,
        version = version
)