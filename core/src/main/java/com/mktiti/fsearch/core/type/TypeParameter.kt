package com.mktiti.fsearch.core.type

data class TypeParameter(
    val sign: String,
    val bounds: TypeBounds
) {

    fun apply(params: List<ApplicationParameter.Substitution>): TypeParameter? {
        return copy(
            bounds = bounds.apply(params) ?: return null
        )
    }

    fun fits(type: Type.NonGenericType): TypeBoundFit = bounds.fits(type)

    override fun toString() = buildString {
        append(sign)
        with(bounds) {
            if (upperBounds.isNotEmpty()) {
                val upperString = upperBounds.joinToString(prefix = " extends ", separator = " & ")
                append(upperString)
            }
        }
    }

}
