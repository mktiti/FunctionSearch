package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.SuperType.StaticSuper
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.type.TypeInfo
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.util.defaultRepo

data class QueryType(
    val inputParameters: List<NonGenericType>,
    val output: NonGenericType
) {

    companion object {
        private val funTypeMap: MutableMap<Int, TypeTemplate> = mutableMapOf()

        private fun createFunType(paramCount: Int): TypeTemplate {
            return TypeTemplate(
                    info = TypeInfo("\$QueryFunArg_$paramCount", emptyList(), "", virtual = true),
                    typeParams = (0..paramCount).map { TypeParameter(('A' + it).toString(), TypeBounds(emptySet())) },
                    superTypes = emptyList(),
                    samType = null
            )
        }

        // TODO
        fun functionType(inputs: List<NonGenericType>, output: NonGenericType): NonGenericType {
            return funTypeMap.computeIfAbsent(inputs.size, this::createFunType).staticApply(inputs + output)!!
        }

        fun virtualType(name: String, supers: List<NonGenericType>): DirectType = DirectType(
                info = TypeInfo(
                        name = name,
                        packageName = emptyList(),
                        artifact = "",
                        virtual = true
                ),
                superTypes = supers.map { StaticSuper.EagerStatic(it) },
                samType = null
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


