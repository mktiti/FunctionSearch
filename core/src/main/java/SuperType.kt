import Type.*
import Type.NonGenericType.*
import SuperType.StaticSuper.*

sealed class SuperType<out T : Type> {

    abstract val type: T

    abstract fun staticApply(args: List<NonGenericType>): SuperType<NonGenericType>?
    abstract fun dynamicApply(args: List<ApplicationParameter>): SuperType<Type>?

    sealed class StaticSuper : SuperType<NonGenericType>() {

        override fun staticApply(args: List<NonGenericType>): StaticSuper = this

        override fun dynamicApply(args: List<ApplicationParameter>): SuperType<Type>? = this

        class EagerStatic(
            override val type: NonGenericType
        ) : StaticSuper()

        class LazySat(
            val base: () -> Applicable,
            private val args: List<NonGenericType>
        ) : StaticSuper() {

            override val type: StaticAppliedType by lazy {
                base().forceStaticApply(args)
            }

        }

    }

    sealed class DynamicSuper : SuperType<DynamicAppliedType>() {

        abstract override fun staticApply(args: List<NonGenericType>): StaticSuper?

        abstract override fun dynamicApply(args: List<ApplicationParameter>): DynamicSuper?

        class EagerDynamic(
                override val type: DynamicAppliedType
        ) : DynamicSuper() {

            override fun staticApply(args: List<NonGenericType>): StaticSuper? {
                return LazySat(type.asConst(), args)
            }

            override fun dynamicApply(args: List<ApplicationParameter>): DynamicSuper? {
                return LazyDat(type.asConst(), args)
            }

        }

        class LazyDat(
                val base: () -> Applicable,
                private val args: List<ApplicationParameter>
        ) : DynamicSuper() {

            override val type: DynamicAppliedType by lazy {
                base().forceDynamicApply(args)
            }

            override fun staticApply(args: List<NonGenericType>): StaticSuper? {
                return LazySat(type.asConst(), args)
            }

            override fun dynamicApply(args: List<ApplicationParameter>): DynamicSuper? {
                return LazyDat(type.asConst(), args)
            }

        }

    }

}