package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.type.TypeParameter

sealed class SubResult {
    object Failure : SubResult()

    sealed class Continue : SubResult() {
        object ConstraintsKept : Continue()

        object Skip : Continue()
    }

    data class TypeArgUpdate(val arg: Int, val type: TypeHolder.Static) : SubResult()
}

interface JavaParamFitter {

    fun subStatic(
            argPar: TypeHolder.Static,
            subPar: TypeHolder.Static,
            variance: InheritanceLogic
    ): Boolean

    fun subDynamic(
            argCtx: List<TypeParameter>,
            argPar: TypeHolder.Dynamic,
            subPar: TypeHolder.Static,
            variance: InheritanceLogic
    ): SubResult

    fun subAny(
            context: List<TypeParameter>,
            argPar: ApplicationParameter,
            subType: TypeHolder.Static,
            variance: InheritanceLogic
    ): SubResult

}
