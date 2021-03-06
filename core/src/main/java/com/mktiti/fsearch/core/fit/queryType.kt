package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.repo.JavaInfoRepo
import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.forceStaticApply

data class QueryType(
    val inputParameters: List<NonGenericType>,
    val output: NonGenericType
) {

    companion object {
        fun wildcard(infoRepo: JavaInfoRepo) = DirectType(
                minInfo = MinimalInfo.anyWildcard,
                superTypes = listOf(infoRepo.objectType.complete().holder()),
                samType = null,
                virtual = true
        )

        fun functionType(inputs: List<NonGenericType>, output: NonGenericType, infoRepo: JavaInfoRepo): NonGenericType {
            val funTemplate = TypeTemplate(
                    info = infoRepo.funInfo(inputs.size),
                    typeParams = (0..inputs.size).map { TypeParameter(('A' + it).toString(), TypeBounds(emptySet())) },
                    superTypes = emptyList(),
                    samType = null,
                    virtual = true
            )

            return funTemplate.forceStaticApply(TypeHolder.staticDirects(inputs + output))
        }

        fun virtualType(name: String, supers: List<TypeHolder.Static>): DirectType = DirectType(
                minInfo = MinimalInfo(
                        simpleName = name,
                        packageName = emptyList()
                ),
                superTypes = supers,
                samType = null,
                virtual = true
        )
     }

    val allParams by lazy {
        inputParameters + output
    }

    override fun toString() = buildString {
        append(inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ", transform = NonGenericType::fullName))
        append(output.fullName)
    }
}


