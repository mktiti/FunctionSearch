package com.mktiti.fsearch.model.build.intermediate

import kotlinx.serialization.Serializable

@Serializable
data class TypeInfoResult(
        val directInfos: List<SemiInfo.DirectInfo>,
        val templateInfos: List<SemiInfo.TemplateInfo>
)

@Serializable
data class FunctionInfoResult(
        val staticFunctions: List<RawFunInfo>,
        val instanceMethods: List<Pair<IntMinInfo, List<RawFunInfo>>>
)

@Serializable
data class ArtifactInfoResult(
        val typeInfo: TypeInfoResult,
        val funInfo: FunctionInfoResult
)

