package com.mktiti.fsearch.model.build.service

import com.mktiti.fsearch.model.build.intermediate.IntMinInfo
import com.mktiti.fsearch.model.build.intermediate.SemiInfo
import com.mktiti.fsearch.model.build.intermediate.TemplateTypeParamInfo

class TypeInfoTypeParamResolver(
        private val templateInfos: Collection<SemiInfo.TemplateInfo>
) : TypeParamResolver {
    override fun typeParams(info: IntMinInfo): List<TemplateTypeParamInfo>? {
        return templateInfos.find { it.info == info }?.typeParams
    }
}

class CombinedTypeParamResolver(
        private val resolvers: Collection<TypeParamResolver>
) : TypeParamResolver {

    constructor(vararg resolvers: TypeParamResolver) : this(resolvers.toList())

    override fun typeParams(info: IntMinInfo): List<TemplateTypeParamInfo>? {
        return resolvers.asSequence().mapNotNull {
            it.typeParams(info)
        }.firstOrNull()
    }

}