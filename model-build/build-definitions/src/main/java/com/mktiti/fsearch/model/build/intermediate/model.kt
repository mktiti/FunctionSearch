package com.mktiti.fsearch.model.build.intermediate

data class TypeInfoResult(
        val directInfos: List<SemiInfo.DirectInfo>,
        val templateInfos: List<SemiInfo.TemplateInfo>
)

data class IntInstanceFunEntry(
        val info: IntMinInfo,
        val funInfos: List<RawFunInfo>
)

data class FunctionInfoResult(
        val staticFunctions: List<RawFunInfo>,
        val instanceMethods: List<IntInstanceFunEntry>
)

data class ArtifactInfoResult(
        val typeInfo: TypeInfoResult,
        val funInfo: FunctionInfoResult
)

