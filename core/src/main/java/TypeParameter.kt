
data class TypeParameter(
    val sign: String,
    val bounds: TypeBounds
) {

    fun apply(params: List<ApplicationParameter>): TypeParameter? {
        return copy(
            bounds = bounds.apply(params) ?: return null
        )
    }

    fun fits(typeParCtx: List<TypeParameter>, type: Type): TypeBoundFit = bounds.fits(typeParCtx, type)

    override fun toString() = buildString {
        append(sign)
        with(bounds) {
            fun boundName(bound: ApplicationParameter): String = when (bound) {
                is ApplicationParameter.ParamSubstitution -> "#${bound.param}"
                is ApplicationParameter.TypeSubstitution<*> -> bound.type.fullName
            }

            if (upperBounds.isNotEmpty()) {
                val upperString = upperBounds.joinToString(prefix = " extends ", separator = " & ", transform = ::boundName)
                append(upperString)
            }
            if (lowerBounds.isNotEmpty()) {
                val lowerString = lowerBounds.joinToString(prefix = " super ", separator = " & ", transform = ::boundName)
                append(lowerString)
            }
        }
    }

}
