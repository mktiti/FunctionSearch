package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.TypeParameter

sealed class SubResult {
    object Failure : SubResult()

    sealed class Continue : SubResult() {
        object ConstraintsKept : Continue()

        object Skip : Continue()
    }

    data class TypeArgUpdate(val arg: Int, val type: CompleteMinInfo.Static) : SubResult()
}

interface JavaParamFitter {

    fun subStatic(
            argPar: CompleteMinInfo.Static,
            subPar: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): Boolean

    fun subDynamic(
            argCtx: List<TypeParameter>,
            argPar: CompleteMinInfo.Dynamic,
            subPar: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): SubResult

    fun subAny(
            context: List<TypeParameter>,
            argPar: ApplicationParameter,
            subType: CompleteMinInfo.Static,
            variance: InheritanceLogic
    ): SubResult

}
