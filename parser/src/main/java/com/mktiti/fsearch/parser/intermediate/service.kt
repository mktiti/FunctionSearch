package com.mktiti.fsearch.parser.intermediate

import com.mktiti.fsearch.core.type.MinimalInfo

interface ArtifactTypeInfoCollector<in I> {

    fun collectTypeInfo(info: I): TypeInfoResult

}

interface TypeParamResolver {

    fun typeParams(info: MinimalInfo): List<TemplateTypeParamInfo>?

    operator fun get(info: MinimalInfo) = typeParams(info)

}

class TypeInfoTypeParamResolver(private val templateInfos: Collection<SemiInfo.TemplateInfo>) : TypeParamResolver {
    override fun typeParams(info: MinimalInfo): List<TemplateTypeParamInfo>? {
        return templateInfos.find { it.info == info }?.typeParams
    }
}

interface FunctionInfoCollector<in I> {

    fun collectFunctions(info: I, typeParamResolver: TypeParamResolver): FunctionInfoResult

}