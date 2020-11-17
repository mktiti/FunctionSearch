package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.fit.InheritanceLogic

typealias StaticTypeSubstitution = ApplicationParameter.Substitution.TypeSubstitution<CompleteMinInfo.Static, Type.NonGenericType>

sealed class ApplicationParameter {

    sealed class BoundedWildcard(
            val direction: BoundDirection
    ) : ApplicationParameter() {

        enum class BoundDirection(
                val subVariance: InheritanceLogic,
                val keyword: String
        ) {
            UPPER(InheritanceLogic.COVARIANCE, "extends"),
            LOWER(InheritanceLogic.CONTRAVARIANCE, "super")
        }

        abstract val param: Substitution

        val subVariance: InheritanceLogic
            get() = direction.subVariance

        class Static(
                override val param: StaticTypeSubstitution,
                direction: BoundDirection
        ) : BoundedWildcard(direction) {

            override val typeParamCount: Int
                get() = 0

            override fun dynamicApply(typeParams: List<ApplicationParameter>): Static = this

            override fun applySelf(self: TypeHolder.Static): Static = this

        }

        class Dynamic(
                override val param: Substitution,
                direction: BoundDirection
        ) : BoundedWildcard(direction) {

            override val typeParamCount: Int
                get() = param.typeParamCount

            override fun dynamicApply(typeParams: List<ApplicationParameter>): BoundedWildcard? {
                return when (val applied = param.dynamicApply(typeParams)) {
                    null -> null
                    is Substitution -> Dynamic(param = applied, direction = direction)
                    is BoundedWildcard -> if (direction == applied.direction) applied else null
                }
            }

            override fun applySelf(self: TypeHolder.Static) = Dynamic(
                    param = param.applySelf(self),
                    direction = direction
            )

        }

        override fun toString() = "? ${direction.keyword} $param"

    }

    sealed class Substitution : ApplicationParameter(), StaticApplicable {

        data class ParamSubstitution(val param: Int) : Substitution() {

            override val typeParamCount: Int
                get() = param + 1

            override fun staticApply(typeArgs: List<TypeHolder.Static>) = typeArgs.getOrNull(param)

            override fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter? {
                return typeParams.getOrNull(param) ?: return null
            }

            override fun applySelf(self: TypeHolder.Static) = this

            override fun toString() = "#$param"
        }

        object SelfSubstitution : Substitution() {

            override val typeParamCount: Int
                get() = 0

            override fun staticApply(typeArgs: List<TypeHolder.Static>): Nothing? = null

            override fun dynamicApply(typeParams: List<ApplicationParameter>) = this

            override fun applySelf(self: TypeHolder.Static) = TypeSubstitution(self)

            override fun toString() = "\$SELF"
        }

        class TypeSubstitution<out I : CompleteMinInfo<*>, out T : Type<I>>(
                val holder: TypeHolder<I, T>
        ) : Substitution() {

            companion object {
                val unboundedWildcard: StaticTypeSubstitution = TypeSubstitution(TypeHolder.anyWildcard)
            }

            override val typeParamCount: Int
                get() = holder.typeParamCount

            override fun dynamicApply(typeParams: List<ApplicationParameter>): TypeSubstitution<*, *>? {
                return when (holder) {
                    is TypeHolder.Dynamic -> TypeSubstitution(holder.dynamicApply(typeParams) ?: return null)
                    is TypeHolder.Static -> this
                }
            }

            override fun applySelf(self: TypeHolder.Static): TypeSubstitution<*, *> {
                return when (holder) {
                    is TypeHolder.Dynamic -> TypeSubstitution(holder.applySelf(self))
                    is TypeHolder.Static -> this
                }
            }

            override fun staticApply(typeArgs: List<TypeHolder.Static>): TypeHolder.Static? = holder.staticApply(typeArgs)

            override fun toString() = holder.toString()

        }

        abstract override fun applySelf(self: TypeHolder.Static): Substitution

    }

    abstract val typeParamCount: Int

    abstract fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter?

    abstract fun applySelf(self: TypeHolder.Static): ApplicationParameter

}