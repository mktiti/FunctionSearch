package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.backend.ContextId
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.model.build.intermediate.QueryImport
import com.mktiti.fsearch.model.build.intermediate.QueryImports
import com.mktiti.fsearch.modules.ArtifactId

fun ArtifactIdDto.toId() = ArtifactId(group.split('.'), name, version)

fun QueryCtxDto.artifactsId() = ContextId(artifacts.map { it.toId() }.toSet())

fun QueryCtxDto.imports(): QueryImports {
    val imports = imports.map {
        val packageName = it.packageName.split(".")
        if (it.simpleName == "*") {
            QueryImport.PackageImport(packageName)
        } else {
            QueryImport.TypeImport(MinimalInfo(packageName, it.simpleName))
        }
    }
    return QueryImports(imports)
}

fun relationDtoFromModel(rel: FunInstanceRelation): FunRelationDto = when (rel) {
    FunInstanceRelation.INSTANCE -> FunRelationDto.INSTANCE
    FunInstanceRelation.STATIC -> FunRelationDto.STATIC
    FunInstanceRelation.CONSTRUCTOR -> FunRelationDto.CONSTRUCTOR
}

fun MinimalInfo.asDto() = TypeDto(
        packageName = packageName.joinToString("."),
        simpleName = simpleName
)

fun FunctionObj.asDto() = FunId(
        type = info.file.asDto(),
        name = info.name,
        signature = signature.toString(),
        relation = relationDtoFromModel(info.relation)
)

fun SemiType.asDto(): TypeInfoDto = TypeInfoDto(
        type = info.asDto(),
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
