package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.SuperType.StaticSuper
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeInfo

data class QueryType(
    val inputParameters: List<NonGenericType>,
    val output: NonGenericType
) {

    val allParams by lazy {
        inputParameters + output
    }

    override fun toString() = buildString {
        append(inputParameters.joinToString(prefix = "(", separator = ", ", postfix = ") -> ", transform = NonGenericType::fullName))
        append(output.fullName)
    }
}

fun virtualType(name: String, supers: List<NonGenericType>): DirectType = DirectType(
    info = TypeInfo(
            name = name,
            packageName = emptyList(),
            artifact = "",
            virtual = true
    ),
    superTypes = supers.map { StaticSuper.EagerStatic(it) }
)
