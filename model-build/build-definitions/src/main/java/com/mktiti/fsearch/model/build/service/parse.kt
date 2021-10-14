package com.mktiti.fsearch.model.build.service

import com.mktiti.fsearch.core.fit.QueryType
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.model.build.intermediate.*

interface ArtifactTypeInfoCollector<in I> {

    fun collectTypeInfo(info: I): TypeInfoResult

}

interface TypeParamResolver {

    fun typeParams(info: MinimalInfo): List<TemplateTypeParamInfo>? = typeParams(info.toIntMinInfo())

    fun typeParams(info: IntMinInfo): List<TemplateTypeParamInfo>?

    operator fun get(info: MinimalInfo) = typeParams(info)

    operator fun get(info: IntMinInfo) = typeParams(info)

}

class TypeInfoTypeParamResolver(private val templateInfos: Collection<SemiInfo.TemplateInfo>) : TypeParamResolver {
    override fun typeParams(info: IntMinInfo): List<TemplateTypeParamInfo>? {
        return templateInfos.find { it.info == info }?.typeParams
    }
}

interface FunctionInfoCollector<in I> {

    fun collectFunctions(info: I, typeParamResolver: TypeParamResolver): FunctionInfoResult

}

interface QueryParser {

    data class ParseResult(
            val query: QueryType,
            val virtualTypes: List<Type.NonGenericType.DirectType>
    )

    fun parse(query: String, imports: QueryImports): ParseResult
}