package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.ParamSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.SelfSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.DynamicTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution

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
                is DynamicTypeSubstitution -> DynamicTypeSubstitution(bound.type.dynamicApply(params) ?: return null)
            }
        }.toSet()
    }

    fun apply(params: List<Substitution>): TypeBounds? {
        return copy(
                upperBounds = upperBounds.applyAll(params) ?: return null
        )
    }

    // TODO primitive version
    operator fun plus(other: TypeBounds) = TypeBounds(
            upperBounds = upperBounds + other.upperBounds
    )

}

fun upperBounds(vararg bounds: Substitution): TypeBounds = TypeBounds(
        upperBounds = bounds.toSet()
)
