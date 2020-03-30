import ApplicationParameter.Substitution
import ApplicationParameter.Substitution.ParamSubstitution
import ApplicationParameter.Substitution.SelfSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import ApplicationParameter.Wildcard
import Type.DynamicAppliedType
import Type.NonGenericType
import TypeBoundFit.*

sealed class TypeBoundFit {
    object Fit : TypeBoundFit()

    object Unfit : TypeBoundFit()

    object YetUncertain : TypeBoundFit()

    data class Requires(val update: SubResult.TypeArgUpdate) : TypeBoundFit()
}

data class TypeBounds(
    val upperBounds: Set<Substitution>
) {

    private companion object {
        fun Set<Substitution>.applyAll(params: List<Substitution>): Set<Substitution>? = map { bound ->
            when (bound) {
                is SelfSubstitution -> bound
                is ParamSubstitution -> {
                    params.getOrNull(bound.param) ?: return null
                }
                is StaticTypeSubstitution -> bound
                is DynamicTypeSubstitution -> Substitution.TypeSubstitution.wrap(bound.type.forceApply(params))
            }
        }.toSet()

        private fun Boolean.fitOrNot(): TypeBoundFit = if (this) Fit else Unfit
    }

    fun apply(params: List<Substitution>): TypeBounds? {
        return TypeBounds(
            upperBounds = upperBounds.applyAll(params) ?: return null
        )
    }

    private fun commonBase(bound: DynamicAppliedType, type: NonGenericType, variance: InheritanceLogic): Pair<DynamicAppliedType, NonGenericType>? {
        return if (type.info == bound.info) {
            bound to type
        } else {
            when (variance) {
                InheritanceLogic.INVARIANCE -> null
                InheritanceLogic.COVARIANCE -> type.superTypes.asSequence().mapNotNull { commonBase(bound, it.type, variance) }.firstOrNull()
                InheritanceLogic.CONTRAVARIANCE -> bound.superTypes.asSequence().mapNotNull { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> null
                        is SuperType.DynamicSuper -> commonBase(superType.type, type, variance)
                    }
                }.firstOrNull()
            }
        }
    }

    private fun fitsDatBound(self: NonGenericType, upperBound: DynamicAppliedType, type: NonGenericType): TypeBoundFit {
        val assumedSelf = upperBound.applySelf(self)

        val (boundBase: Type, typeBase: NonGenericType) = commonBase(assumedSelf, type, InheritanceLogic.COVARIANCE) ?: return Unfit

        val paramPairs = boundBase.typeArgMapping.zipIfSameLength(typeBase.typeArgs) ?: return Unfit

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
                    subDynamic(emptyList(), param.type, arg, InheritanceLogic.INVARIANCE).asResult()
                }
                is StaticTypeSubstitution -> subStatic(param.type, arg, InheritanceLogic.INVARIANCE).fitOrNot()

                Wildcard.Direct -> Fit
                is Wildcard.BoundedWildcard -> {
                    subAny(emptyList(), param, arg, param.subVariance).asResult()
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

    private fun fitsBound(upperBound: Substitution, type: NonGenericType): TypeBoundFit {
        return when (upperBound) {
            is SelfSubstitution -> Fit
            is ParamSubstitution -> YetUncertain
            is DynamicTypeSubstitution -> fitsDatBound(type, upperBound.type, type)
            is StaticTypeSubstitution -> type.anyNgSuperInclusive { it == upperBound.type }.fitOrNot()
        }
    }

    fun fits(type: NonGenericType): TypeBoundFit =
         upperBounds.asSequence()
            .map { fitsBound(it, type) }
            .filter { it != Fit }
            .firstOrNull() ?: Fit

    // TODO primitive version
    operator fun plus(other: TypeBounds) = TypeBounds(
        upperBounds = upperBounds + other.upperBounds
    )

}

fun upperBounds(vararg bounds: Substitution): TypeBounds = TypeBounds(
    upperBounds = bounds.toSet()
)
