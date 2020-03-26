import Type.DynamicAppliedType

data class TypeBounds(
    val lowerBounds: Set<Type>, // super
    val upperBounds: Set<Type>  // extends
) {

    private companion object {
        fun Set<Type>.applyAll(params: List<ApplicationParameter>): Set<Type> = map { bound ->
            when (bound) {
                is Type.NonGenericType -> bound
                is DynamicAppliedType -> bound.forceApply(params)
            }
        }.toSet()
    }

    fun apply(params: List<ApplicationParameter>) = TypeBounds(
        lowerBounds = lowerBounds.applyAll(params),
        upperBounds = upperBounds.applyAll(params)
    )

    private fun fitsLowerBound(lowerBound: Type, type: Type): Boolean = when (lowerBound) {
        is Type.NonGenericType -> lowerBound.anyNgSuperInclusive { type == it }
        is DynamicAppliedType -> TODO()
    }

    private fun fitsUpperBound(upperBound: Type, type: Type): Boolean =  when (type) {
        is Type.NonGenericType -> type.anyNgSuperInclusive { it == upperBound }
        is DynamicAppliedType -> TODO()
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

data class TypeParameter(
    val sign: String,
    val bounds: TypeBounds
) {

    fun apply(params: List<ApplicationParameter>): TypeParameter = copy(
        bounds = bounds.apply(params)
    )

    fun fits(type: Type) = bounds.contains(type)

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
