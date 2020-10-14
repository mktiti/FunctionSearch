package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.fit.InheritanceLogic

typealias StaticTypeSubstitution = ApplicationParameter.Substitution.TypeSubstitution<CompleteMinInfo.Static, Type.NonGenericType>

sealed class ApplicationParameter {

    sealed class Wildcard : ApplicationParameter() {
        object Direct : Wildcard() {
            override fun dynamicApply(typeParams: List<ApplicationParameter>) = this

            override fun applySelf(self: TypeHolder.Static) = this

            override fun toString() = "?"
        }

        data class Bounded(
                val param: Substitution,
                private val direction: BoundDirection
        ) : Wildcard() {

            enum class BoundDirection(
                    val subVariance: InheritanceLogic,
                    val keyword: String
            ) {
                UPPER(InheritanceLogic.COVARIANCE, "extends"),
                LOWER(InheritanceLogic.CONTRAVARIANCE, "super")
            }

            val subVariance: InheritanceLogic
                get() = direction.subVariance

            override fun dynamicApply(typeParams: List<ApplicationParameter>): Wildcard? {
                return when (val applied = param.dynamicApply(typeParams)) {
                    null -> null
                    is Substitution -> copy(param = applied)
                    Direct -> Direct
                    is Bounded -> if (direction == applied.direction) applied else null
                }
            }

            override fun applySelf(self: TypeHolder.Static) = copy(
                    param = param.applySelf(self)
            )

            override fun toString() = "? ${direction.keyword} $param"
        }
    }

    sealed class Substitution : ApplicationParameter(), StaticApplicable {

        data class ParamSubstitution(val param: Int) : Substitution() {
            override fun staticApply(typeArgs: List<TypeHolder.Static>) = typeArgs.getOrNull(param)

            override fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter? {
                return typeParams.getOrNull(param) ?: return null
            }

            override fun applySelf(self: TypeHolder.Static) = this

            override fun toString() = "#$param"
        }

        object SelfSubstitution : Substitution() {
            override fun staticApply(typeArgs: List<TypeHolder.Static>): Nothing? = null

            override fun dynamicApply(typeParams: List<ApplicationParameter>) = this

            override fun applySelf(self: TypeHolder.Static) = TypeSubstitution(self)

            override fun toString() = "\$SELF"
        }

        class TypeSubstitution<out I : CompleteMinInfo<*>, out T : Type<I>>(
                val holder: TypeHolder<I, T>
        ) : Substitution() {

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
        /*
        sealed class TypeSubstitution<out T : Type<*>, out I : CompleteMinInfo<*>>(
            val type: I
        ) : Substitution() {

            companion object {
                fun wrap(info: CompleteMinInfo<*>): TypeSubstitution<*, *> = when (info) {
                    is CompleteMinInfo.Static -> StaticTypeSubstitution(info)
                    is CompleteMinInfo.Dynamic -> DynamicTypeSubstitution(info)
                }

                /*
                fun wrap(type: Type): TypeSubstitution<*, *> = when (type) {
                    is NonGenericType -> StaticTypeSubstitution(type)
                    is DynamicAppliedType -> DynamicTypeSubstitution(type)
                }
                 */
            }

            class DynamicTypeSubstitution(type: CompleteMinInfo.Dynamic) : TypeSubstitution<DynamicAppliedType, CompleteMinInfo.Dynamic>(type) {
                override fun staticApply(typeArgs: List<CompleteMinInfo.Static>) = type.staticApply(typeArgs)

                override fun dynamicApply(typeParams: List<ApplicationParameter>): DynamicTypeSubstitution? {
                    return type.dynamicApply(typeParams)?.let { DynamicTypeSubstitution(it) }
                }

                override fun applySelf(self: TypeHolder.Static) = DynamicTypeSubstitution(type.applySelf(self))
            }

            class StaticTypeSubstitution(type: CompleteMinInfo.Static) : TypeSubstitution<NonGenericType, CompleteMinInfo.Static>(type) {
                override fun staticApply(typeArgs: List<CompleteMinInfo.Static>) = type

                override fun dynamicApply(typeParams: List<ApplicationParameter>): StaticTypeSubstitution = this
            }

            override fun applySelf(self: TypeHolder.Static) = this

            override fun toString() = type.toString()

        }

        abstract fun staticApply(typeArgs: List<CompleteMinInfo.Static>): CompleteMinInfo.Static?

        abstract override fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter?

        abstract override fun applySelf(self: TypeHolder.Static): Substitution

    }

         */
    abstract fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter?

    abstract fun applySelf(self: TypeHolder.Static): ApplicationParameter

}