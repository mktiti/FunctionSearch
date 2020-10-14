package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.TypeBoundFit.*
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.*
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.type.TypeHolder
import com.mktiti.fsearch.core.util.SuperUtil
import com.mktiti.fsearch.core.util.zipIfSameLength

class JavaTypeBoundFitter(
        private val paramFitter: JavaParamFitter,
        private val typeResolver: TypeResolver
) {

    private companion object {
        private fun Boolean.fitOrNot(): TypeBoundFit = if (this) Fit else Unfit
    }

    fun fits(typeBounds: TypeBounds, type: TypeHolder.Static): TypeBoundFit =
            typeBounds.upperBounds.asSequence()
                    .map { fitsBound(it, type) }
                    .filter { it != Fit }
                    .firstOrNull() ?: Fit

    private fun commonBase(bound: TypeHolder.Dynamic, type: TypeHolder.Static, variance: InheritanceLogic): Pair<TypeHolder.Dynamic, TypeHolder.Static>? {
        return if (type.info.base == bound.info.base) {
            bound to type
        } else {
            when (variance) {
                InheritanceLogic.INVARIANCE -> null
                InheritanceLogic.COVARIANCE -> {
                    val resolvedType = type.with(typeResolver) ?: return null
                    resolvedType.superTypes.asSequence().mapNotNull {
                        commonBase(bound, it, variance)
                    }.firstOrNull()
                }
                InheritanceLogic.CONTRAVARIANCE -> {
                    val resolvedBound = bound.with(typeResolver) ?: return null
                    resolvedBound.superTypes.asSequence().mapNotNull { superType ->
                        when (superType) {
                            is TypeHolder.Static -> null
                            is TypeHolder.Dynamic -> commonBase(superType, type, variance)
                        }
                    }.firstOrNull()
                }
            }
        }
    }

    private fun fitsDatBound(self: TypeHolder.Static, upperBound: TypeHolder.Dynamic, type: TypeHolder.Static): TypeBoundFit {
        val assumedSelf = upperBound.applySelf(self)

        val (boundBase: TypeHolder.Dynamic, typeBase: TypeHolder.Static) = commonBase(assumedSelf, type, InheritanceLogic.COVARIANCE) ?: return Unfit
        val resolvedBoundBase = boundBase.with(typeResolver) ?: return Unfit
        val resolvedTypeBase = typeBase.with(typeResolver) ?: return Unfit

        val paramPairs = resolvedBoundBase.typeArgMapping.zipIfSameLength(resolvedTypeBase.typeArgs) ?: return Unfit

        fun SubResult.asResult(): TypeBoundFit = when (this) {
            SubResult.Failure -> Unfit
            SubResult.Continue.ConstraintsKept -> Fit
            SubResult.Continue.Skip -> YetUncertain
            is SubResult.TypeArgUpdate -> YetUncertain
        }

        val results: List<TypeBoundFit> = paramPairs.map { (param, arg) ->
            when (param) {
                is ParamSubstitution -> Requires(SubResult.TypeArgUpdate(param.param, arg))
                SelfSubstitution -> Unfit
                is TypeSubstitution<*, *> -> {
                    when (val holder = param.holder) {
                        is TypeHolder.Dynamic -> paramFitter.subDynamic(emptyList(), holder, arg, InheritanceLogic.INVARIANCE).asResult()
                        is TypeHolder.Static -> paramFitter.subStatic(holder, arg, InheritanceLogic.INVARIANCE).fitOrNot()
                    }
                }
                ApplicationParameter.Wildcard.Direct -> Fit
                is ApplicationParameter.Wildcard.Bounded -> {
                    paramFitter.subAny(emptyList(), param, arg, param.subVariance).asResult()
                }
            }
        }

        val requires = results.find { it is Requires }
        return when {
            results.contains(Unfit) -> Unfit
            requires != null -> requires
            results.contains(YetUncertain) -> YetUncertain
            else -> Fit
        }
    }

    private fun fitsBound(upperBound: Substitution, type: TypeHolder.Static): TypeBoundFit {
        return when (upperBound) {
            is SelfSubstitution -> Fit
            is ParamSubstitution -> YetUncertain
            is TypeSubstitution<*, *> -> {
                when (val holder = upperBound.holder) {
                    is TypeHolder.Dynamic -> fitsDatBound(type, holder, type)
                    is TypeHolder.Static -> {
                        SuperUtil.anyNgSuper(typeResolver, type) {
                            it.completeInfo == holder.info
                        }.fitOrNot()
                    }
                }
            }
        }
    }

}

sealed class TypeBoundFit {
    object Fit : TypeBoundFit()

    object Unfit : TypeBoundFit()

    object YetUncertain : TypeBoundFit()

    data class Requires(val update: SubResult.TypeArgUpdate) : TypeBoundFit()
}