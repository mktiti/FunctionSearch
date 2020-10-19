package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.util.genericString

interface QueryFitter {

    fun fitsOrderedQuery(query: QueryType, function: FunctionObj): FittingMap?

    fun fitsQuery(query: QueryType, function: FunctionObj): FittingMap?

}

data class FittingMap(
        val orderedQuery: QueryType,
        val funSignature: TypeSignature,
        val typeParamMapping: List<Pair<TypeParameter, Type.NonGenericType>> = emptyList()
) {

    operator fun plus(typeParam: Pair<TypeParameter, Type.NonGenericType>): FittingMap = copy(
        typeParamMapping = typeParamMapping + typeParam
    )

    override fun toString() = buildString {
        append("Query fit, where ")
        append(typeParamMapping.genericString { (param, type) -> "${param.sign} = $type" })
        val inputPairs = funSignature.inputParameters.zip(orderedQuery.inputParameters)
        val paramMap = inputPairs.joinToString(prefix = " (", separator = ", ", postfix = ") -> ") { (funIn, queryIn) ->
            "${funIn.first}: $queryIn"
        }
        append(paramMap)
        append(orderedQuery.output)
    }

}