import Type.DynamicAppliedType
import Type.NonGenericType.DirectType
import Type.NonGenericType.StaticAppliedType

data class TypeBounds(
    val lowerBounds: Set<Type> = emptySet(), // super
    val upperBounds: Set<Type> = emptySet()  // extends
) {

    private fun fitsLowerBound(lowerBound: Type, type: Type): Boolean = when (lowerBound) {
        is DirectType -> TODO()
        is StaticAppliedType -> TODO()
        is DynamicAppliedType -> TODO()
    }

    private fun fitsUpperBound(lowerBound: Type, type: Type): Boolean {



        return false
    }

    operator fun contains(type: Type): Boolean =
        lowerBounds.all { fitsLowerBound(it, type) } &&
        upperBounds.all { fitsUpperBound(it, type) }

    // TODO primitive version
    operator fun plus(other: TypeBounds) = TypeBounds(
        lowerBounds = lowerBounds + other.lowerBounds,
        upperBounds = upperBounds + other.upperBounds
    )

}

class TypeParameter(
    val sign: String,
    val bounds: TypeBounds = TypeBounds()
) {

    override fun toString() = buildString {
        append(sign)
        with(bounds) {
            if (upperBounds.isNotEmpty()) {
                val upperString = upperBounds.joinToString(prefix = " extends ", separator = " & ") { it.fullName }
                append(upperString)
            }
            if (lowerBounds.isNotEmpty()) {
                lowerBounds.joinToString(prefix = " super ", separator = " & ") { it.fullName }
                append(lowerBounds)
            }
        }
    }

}
