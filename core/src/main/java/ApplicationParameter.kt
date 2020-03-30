import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import Type.DynamicAppliedType
import Type.NonGenericType

sealed class ApplicationParameter {

    sealed class Wildcard : ApplicationParameter() {
        object Direct : Wildcard() {
            override fun dynamicApply(typeParams: List<ApplicationParameter>) = this

            override fun applySelf(self: NonGenericType) = this

            override fun toString() = "?"
        }

        sealed class BoundedWildcard(val param: Substitution) : Wildcard() {
            abstract val subVariance: InheritanceLogic

            class UpperBound(param: Substitution) : BoundedWildcard(param) {
                override val subVariance: InheritanceLogic
                    get() = InheritanceLogic.COVARIANCE

                override fun dynamicApply(typeParams: List<ApplicationParameter>): Wildcard? {
                    return when (val applied = param.dynamicApply(typeParams)) {
                        null -> null
                        is Substitution -> UpperBound(applied)
                        Direct -> Direct
                        is UpperBound -> applied
                        is LowerBound -> null
                    }
                }

                override fun applySelf(self: NonGenericType) = UpperBound(
                    param = param.applySelf(self)
                )

                override fun toString() = "? extends $param"
            }

            class LowerBound(param: Substitution) : BoundedWildcard(param) {
                override val subVariance: InheritanceLogic
                    get() = InheritanceLogic.CONTRAVARIANCE

                override fun dynamicApply(typeParams: List<ApplicationParameter>): Wildcard? {
                    return when (val applied = param.dynamicApply(typeParams)) {
                        null -> null
                        is Substitution -> LowerBound(applied)
                        Direct -> Direct
                        is UpperBound -> null
                        is LowerBound -> applied
                    }
                }

                override fun applySelf(self: NonGenericType) = LowerBound(
                    param = param.applySelf(self)
                )

                override fun toString() = "? super $param"
            }
        }
    }

    sealed class Substitution : ApplicationParameter() {

        data class ParamSubstitution(val param: Int) : Substitution() {
            override fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter? {
                return typeParams.getOrNull(param) ?: return null
            }

            override fun applySelf(self: NonGenericType) = this

            override fun toString() = "#$param"
        }

        object SelfSubstitution : Substitution() {
            override fun dynamicApply(typeParams: List<ApplicationParameter>) = this

            override fun applySelf(self: NonGenericType) = StaticTypeSubstitution(self)

            override fun toString() = "\$SELF"
        }

        sealed class TypeSubstitution<T : Type>(
            val type: T
        ) : Substitution() {

            companion object {
                fun wrap(type: Type): TypeSubstitution<*> = when (type) {
                    is NonGenericType -> StaticTypeSubstitution(type)
                    is DynamicAppliedType -> DynamicTypeSubstitution(type)
                }
            }

            class DynamicTypeSubstitution(type: DynamicAppliedType) : TypeSubstitution<DynamicAppliedType>(type) {
                override fun dynamicApply(typeParams: List<ApplicationParameter>): DynamicTypeSubstitution? {
                    return type.dynamicApply(typeParams)?.let { DynamicTypeSubstitution(it) }
                }

                override fun applySelf(self: NonGenericType) = DynamicTypeSubstitution(type.applySelf(self))
            }

            class StaticTypeSubstitution(type: NonGenericType) : TypeSubstitution<NonGenericType>(type) {
                override fun dynamicApply(typeParams: List<ApplicationParameter>): StaticTypeSubstitution = this
            }

            override fun applySelf(self: NonGenericType) = this

            override fun toString() = type.fullName

        }

        abstract override fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter?

        abstract override fun applySelf(self: NonGenericType): Substitution

    }

    abstract fun dynamicApply(typeParams: List<ApplicationParameter>): ApplicationParameter?

    abstract fun applySelf(self: NonGenericType): ApplicationParameter

}