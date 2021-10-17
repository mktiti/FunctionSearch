package com.mktiti.fsearch.model.build.intermediate

data class TypeInfoResult(
        val directInfos: List<SemiInfo.DirectInfo>,
        val templateInfos: List<SemiInfo.TemplateInfo>
)

data class FunctionInfoResult(
        val staticFunctions: List<RawFunInfo>,
        val instanceMethods: List<Pair<IntMinInfo, List<RawFunInfo>>>
)

data class ArtifactInfoResult(
        val typeInfo: TypeInfoResult,
        val funInfo: FunctionInfoResult
)

