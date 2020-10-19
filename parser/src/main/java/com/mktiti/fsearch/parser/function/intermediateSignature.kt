package com.mktiti.fsearch.parser.function

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.PrimitiveType

class ImTypeParam(
    val sign: String,
    val bounds: List<ImParam>
) {

    val referencedTypeParams by lazy {
        bounds.flatMap { it.referencedTypeParams() }.toSet()
    }

}

sealed class ImParam {

    object Wildcard : ImParam()

    data class Primitive(val value: PrimitiveType) : ImParam()

    data class UpperWildcard(val param: ImParam) : ImParam() {
        override fun referencedTypeParams() = param.referencedTypeParams()
    }

    data class LowerWildcard(val param: ImParam) : ImParam() {
        override fun referencedTypeParams() = param.referencedTypeParams()
    }

    data class Array(val type: ImParam) : ImParam() {
        override fun referencedTypeParams() = type.referencedTypeParams()
    }

    data class Type(
            val info: MinimalInfo,
            val typeArgs: List<ImParam>
    ) : ImParam() {
        override fun referencedTypeParams() = typeArgs.flatMap { it.referencedTypeParams() }.toSet()
    }

    data class TypeParamRef(val sign: String) : ImParam() {
        override fun referencedTypeParams() = setOf(sign)
    }

    object Void : ImParam()

    open fun referencedTypeParams(): Set<String> = emptySet()

}

data class ImSignature(
    val name: String,
    val typeParams: List<ImTypeParam>,
    val inputs: List<Pair<String?, ImParam>>,
    val output: ImParam
)