package com.mktiti.fsearch.backend.api

import com.mktiti.fsearch.backend.ContextId
import com.mktiti.fsearch.core.fit.FunInstanceRelation
import com.mktiti.fsearch.core.fit.FunctionObj
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.dto.*
import com.mktiti.fsearch.modules.ArtifactId

fun ArtifactIdDto.toId() = ArtifactId(group.split('.'), name, version)

fun QueryCtxDto.toId() = ContextId(artifacts.map { it.toId() }.toSet())

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
