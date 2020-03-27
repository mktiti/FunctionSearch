import ApplicationParameter.ParamSubstitution
import ApplicationParameter.TypeSubstitution
import ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import Type.DynamicAppliedType
import Type.NonGenericType
import TypeBoundFit.Companion.fromBool
import TypeBoundFit.FIT
import TypeBoundFit.YET_UNCERTAIN

enum class TypeBoundFit {
    FIT, UNFIT, YET_UNCERTAIN;

    companion object {
        fun fromBool(value: Boolean) = if (value) FIT else UNFIT
    }
}

data class TypeBounds(
    val lowerBounds: Set<ApplicationParameter>, // super
    val upperBounds: Set<ApplicationParameter>  // extends
) {

    private companion object {
        fun Set<ApplicationParameter>.applyAll(params: List<ApplicationParameter>): Set<ApplicationParameter>? = map { bound ->
            when (bound) {
                is ParamSubstitution -> {
                    params.getOrNull(bound.param) ?: return null
                }
                is StaticTypeSubstitution -> bound
                is DynamicTypeSubstitution -> TypeSubstitution.wrap(bound.type.forceApply(params))
            }
        }.toSet()
    }

    fun apply(params: List<ApplicationParameter>): TypeBounds? {
        return TypeBounds(
            lowerBounds = lowerBounds.applyAll(params) ?: return null,
            upperBounds = upperBounds.applyAll(params) ?: return null
        )
    }

    private fun fitsLowerNgBound(lowerBound: NonGenericType, type: Type): Boolean
        = lowerBound.anyNgSuperInclusive { type == it }

    private fun fitsLowerBound(typeParCtx: List<TypeParameter>, lowerBound: ApplicationParameter, type: Type): TypeBoundFit {
        return when (lowerBound) {
            is ParamSubstitution -> YET_UNCERTAIN
            is DynamicTypeSubstitution -> TODO()
            is StaticTypeSubstitution -> fromBool(fitsLowerNgBound(lowerBound.type, type))
        }
    }

    private fun fitsUpperNgBound(upperBound: NonGenericType, type: Type): Boolean = when (type) {
        is NonGenericType -> type.anyNgSuperInclusive { it == upperBound }
        is DynamicAppliedType -> TODO()
    }

    private fun fitsUpperBound(typeParCtx: List<TypeParameter>, upperBound: ApplicationParameter, type: Type): TypeBoundFit {
        return when (upperBound) {
            is ParamSubstitution -> YET_UNCERTAIN
            is DynamicTypeSubstitution -> TODO()
            is StaticTypeSubstitution -> fromBool(fitsUpperNgBound(upperBound.type, type))
        }
    }

    fun fits(typeParCtx: List<TypeParameter>, type: Type): TypeBoundFit =
        (lowerBounds.asSequence().map { fitsLowerBound(typeParCtx, it, type) } +
         upperBounds.asSequence().map { fitsUpperBound(typeParCtx, it, type) })
        .filter { it != FIT }.firstOrNull() ?: FIT

    // TODO primitive version
    operator fun plus(other: TypeBounds) = TypeBounds(
        lowerBounds = lowerBounds + other.lowerBounds,
        upperBounds = upperBounds + other.upperBounds
    )

}

fun lowerBounds(vararg bounds: ApplicationParameter): TypeBounds = TypeBounds(
    upperBounds = emptySet(),
    lowerBounds = bounds.toSet()
)

fun upperBounds(vararg bounds: ApplicationParameter): TypeBounds = TypeBounds(
    upperBounds = bounds.toSet(),
    lowerBounds = emptySet()
)
