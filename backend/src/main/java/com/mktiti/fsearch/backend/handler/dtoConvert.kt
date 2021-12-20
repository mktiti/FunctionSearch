package com.mktiti.fsearch.backend.handler

import com.mktiti.fsearch.backend.context.ContextId
import com.mktiti.fsearch.backend.stats.SearchLog
import com.mktiti.fsearch.backend.stats.SearchResult
import com.mktiti.fsearch.backend.user.Level.NORMAL
import com.mktiti.fsearch.backend.user.Level.PREMIUM
import com.mktiti.fsearch.backend.user.UserInfo
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.model.build.intermediate.QueryImport
import com.mktiti.fsearch.model.build.intermediate.QueryImport.PackageImport
import com.mktiti.fsearch.model.build.intermediate.QueryImport.TypeImport
import com.mktiti.fsearch.model.build.intermediate.QueryImports
import com.mktiti.fsearch.modules.ArtifactId
import com.mktiti.fsearch.rest.api.Role
import com.mktiti.fsearch.dto.Role as DtoRole
import com.mktiti.fsearch.dto.SearchLog as SearchLogDto
import com.mktiti.fsearch.dto.UserInfo as UserInfoDto

fun ArtifactIdDto.toId() = ArtifactId(group.split('.'), name, version)

fun ArtifactId.toDto() = ArtifactIdDto(
        group = group.joinToString("."),
        name = name,
        version = version
)

fun QueryCtxDto.artifactsId() = ContextId(artifacts.map { it.toId() }.toSet())

fun QueryCtxDto.imports(): QueryImports {
    val imports = imports.map {
        val packageName = it.packageName.split(".")
        if (it.simpleName == "*") {
            PackageImport(packageName)
        } else {
            TypeImport(MinimalInfo(packageName, it.simpleName))
        }
    }
    return QueryImports(imports)
}

fun relationDtoFromModel(rel: FunInstanceRelation): FunRelationDto = when (rel) {
    FunInstanceRelation.INSTANCE -> FunRelationDto.INSTANCE
    FunInstanceRelation.STATIC -> FunRelationDto.STATIC
    FunInstanceRelation.CONSTRUCTOR -> FunRelationDto.CONSTRUCTOR
}

fun MinimalInfo.toDto() = TypeDto(
        packageName = packageName.joinToString("."),
        simpleName = simpleName
)

fun FunctionObj.toDto() = FunId(
        type = info.file.toDto(),
        name = info.name,
        signature = signature.toString(),
        relation = relationDtoFromModel(info.relation)
)

fun SemiType.toDto(): TypeInfoDto = TypeInfoDto(
        type = info.toDto(),
        typeParamCount = typeParamCount
)

fun <T> Sequence<T>.limitedResult(limit: Int): ResultList<T> {
    val extraLimit = limit + 1
    val limited = take(extraLimit).toList()
    return if (limited.size == extraLimit) {
        ResultList(limited.take(limit), true)
    } else {
        ResultList(limited, false)
    }
}

fun Role.toDto(): DtoRole = when (this) {
    Role.USER -> DtoRole.USER
    Role.ADMIN -> DtoRole.ADMIN
}

// TODO
fun UserInfo.toDto() = UserInfoDto(
        registerDate = registerDate.toString(),
        level = when (level) {
            NORMAL -> UserLevel.NORMAL
            PREMIUM -> UserLevel.PREMIUM
        }, savedContexts = emptyList()
)

fun QueryImport.toDto(): TypeDto {
    return when (this) {
        is PackageImport -> TypeDto(packageName.joinToString("."), "*")
        is TypeImport -> info.toDto()
    }
}

fun contextToDto(context: ContextId, imports: QueryImports) = QueryCtxDto(
        artifacts = context.artifacts.map { it.toDto() },
        imports = imports.imports.map { it.toDto() }
)

fun SearchLog.toDto() = SearchLogDto(
        request = QueryRequestDto(contextToDto(context, imports), query),
        result = when (result) {
            is SearchResult.Ok -> "Result: ${result.results}"
            SearchResult.QueryError -> "Query Error"
            SearchResult.SearchError -> "Internal Error"
        }
)

fun List<SearchLog>.toDto() = map { it.toDto() }