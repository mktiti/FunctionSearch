package com.mktiti.fsearch.core.fit

import com.mktiti.fsearch.core.fit.TypeBoundFit.*
import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.ApplicationParameter
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.util.zipIfSameLength

class JavaTypeBoundFitter(
        private val paramFitter: JavaParamFitter,
        private val typeResolver: TypeResolver
) {

    private companion object {
        private fun Boolean.fitOrNot(): TypeBoundFit = if (this) Fit else Unfit
    }

    fun fits(typeBounds: TypeBounds, type: CompleteMinInfo.Static): TypeBoundFit =
            typeBounds.upperBounds.asSequence()
                    .map { fitsBound(it, type) }
                    .filter { it != Fit }
                    .firstOrNull() ?: Fit

    private fun CompleteMinInfo.Static.resolve(): NonGenericType? = typeResolver[this]

    private fun CompleteMinInfo.Dynamic.resolve(): DynamicAppliedType? = typeResolver[this]

    private fun commonBase(bound: CompleteMinInfo.Dynamic, type: CompleteMinInfo.Static, variance: InheritanceLogic): Pair<CompleteMinInfo.Dynamic, CompleteMinInfo.Static>? {
        return if (type.base == bound.base) {
            bound to type
        } else {
            when (variance) {
                InheritanceLogic.INVARIANCE -> null
                InheritanceLogic.COVARIANCE -> {
                    val resolvedType = type.resolve() ?: return null
                    resolvedType.superTypes.asSequence().mapNotNull {
                        commonBase(bound, it, variance)
                    }.firstOrNull()
                }
                InheritanceLogic.CONTRAVARIANCE -> {
                    val resolvedBound = bound.resolve() ?: return null
                    resolvedBound.superTypes.asSequence().mapNotNull { superType ->
                        when (superType) {
                            is CompleteMinInfo.Static -> null
                            is CompleteMinInfo.Dynamic -> commonBase(superType, type, variance)
                        }
                    }.firstOrNull()
                }
            }
        }
    }

    private fun fitsDatBound(self: CompleteMinInfo.Static, upperBound: CompleteMinInfo.Dynamic, type: CompleteMinInfo.Static): TypeBoundFit {
        val assumedSelf = upperBound.applySelf(self)

        val (boundBase: CompleteMinInfo<*>, typeBase: CompleteMinInfo.Static) = commonBase(assumedSelf, type, InheritanceLogic.COVARIANCE) ?: return Unfit
        val resolvedBoundBase = boundBase.resolve() ?: return Unfit
        val resolvedTypeBase = typeBase.resolve() ?: return Unfit

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
                is DynamicTypeSubstitution -> {
                    paramFitter.subDynamic(emptyList(), param.type, arg, InheritanceLogic.INVARIANCE).asResult()
                }
                is StaticTypeSubstitution -> {
                    paramFitter.subStatic(param.type, arg, InheritanceLogic.INVARIANCE).fitOrNot()
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

    private fun fitsBound(upperBound: Substitution, type: CompleteMinInfo.Static): TypeBoundFit {
        return when (upperBound) {
            is SelfSubstitution -> Fit
            is ParamSubstitution -> YetUncertain
            is DynamicTypeSubstitution -> fitsDatBound(type, upperBound.type, type)
            is StaticTypeSubstitution -> {
                typeResolver.anyNgSuper(upperBound.type) { it.completeInfo == upperBound.type }.fitOrNot()
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