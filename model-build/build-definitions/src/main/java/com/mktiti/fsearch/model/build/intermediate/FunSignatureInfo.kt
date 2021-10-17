package com.mktiti.fsearch.model.build.intermediate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
sealed class FunSignatureInfo<P> {

    abstract val inputs: List<Pair<String, P>>
    abstract val output: P
    abstract val typeParams: List<TemplateTypeParamInfo>

        data class Direct(
            override val inputs: List<Pair<String, IntStaticCmi>>,
            override val output: IntStaticCmi
    ) : FunSignatureInfo<IntStaticCmi>() {

        override val typeParams: List<TemplateTypeParamInfo>
            get() = emptyList()

    }

        data class Generic(
            override val typeParams: List<TemplateTypeParamInfo>,
            override val inputs: List<Pair<String, TypeParamInfo>>,
            override val output: TypeParamInfo
    ) : FunSignatureInfo<TypeParamInfo>()

}