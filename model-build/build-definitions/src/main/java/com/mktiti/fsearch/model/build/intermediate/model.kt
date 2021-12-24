package com.mktiti.fsearch.model.build.intermediate

data class TypeInfoResult(
        val directInfos: List<SemiInfo.DirectInfo>,
        val templateInfos: List<SemiInfo.TemplateInfo>
) {
    fun toSeq() = TypeInfoSeqResult.simple(directInfos.asSequence(), templateInfos.asSequence())
}

data class IntInstanceFunEntry(
        val info: IntMinInfo,
        val funInfos: List<RawFunInfo>
)

data class FunctionInfoResult(
        val staticFunctions: List<RawFunInfo>,
        val instanceMethods: List<IntInstanceFunEntry>
) {
    fun toSeq() = FunctionInfoSeqResult.simple(staticFunctions.asSequence(), instanceMethods.asSequence())
}

data class ArtifactInfoResult(
        val typeInfo: TypeInfoResult,
        val funInfo: FunctionInfoResult
) {
    fun toSeq() = ArtifactInfoSeqResult.simple(typeInfo.toSeq(), funInfo.toSeq())
}
