package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.util.castIfAllInstance

sealed class TypeHolder<out I : CompleteMinInfo<*>, out T : Type<I>> : StaticApplicable, WeakDynamicApplicable {

    interface DirectHolder<out I : CompleteMinInfo<*>, out T : Type<I>> {
        val type: T
    }

    companion object {
        val anyWildcard = MinimalInfo.anyWildcard.complete().holder()

        fun staticDirects(types: List<NonGenericType>) = types.map { Static.Direct(it) }
        fun dynamicDirects(types: List<DynamicAppliedType>) = types.map { Dynamic.Direct(it) }
        fun anyDirects(types: List<Type<*>>): List<TypeHolder<*, *>> = types.map<Type<*>, TypeHolder<*, *>> {
            when (it) {
                is NonGenericType -> Static.Direct(it)
                is DynamicAppliedType -> Dynamic.Direct(it)
            }
        }

        fun staticDirects(vararg types: NonGenericType) = staticDirects(types.toList())
        fun dynamicDirects(vararg types: DynamicAppliedType) = dynamicDirects(types.toList())
        fun anyDirects(vararg types: Type<*>) = anyDirects(types.toList())

        fun staticIndirects(infos: List<CompleteMinInfo.Static>) = infos.map { Static.Indirect(it) }
        fun dynamicIndirects(infos: List<CompleteMinInfo.Dynamic>) = infos.map { Dynamic.Indirect(it) }
        fun anyIndirects(infos: List<CompleteMinInfo<*>>) = infos.map {
            when (it) {
                is CompleteMinInfo.Static -> Static.Indirect(it)
                is CompleteMinInfo.Dynamic -> Dynamic.Indirect(it)
            }
        }

        fun staticIndirects(vararg infos: CompleteMinInfo.Static) = staticIndirects(infos.toList())
        fun dynamicIndirects(vararg infos: CompleteMinInfo.Dynamic) = dynamicIndirects(infos.toList())
        fun anyIndirects(vararg infos: CompleteMinInfo<*>) = anyIndirects(infos.toList())
    }

    abstract val info: I

    sealed class Dynamic : TypeHolder<CompleteMinInfo.Dynamic, DynamicAppliedType>(), DynamicApplicable {

        data class Indirect(
                override val info: CompleteMinInfo.Dynamic
        ) : Dynamic() {

            override fun with(resolver: TypeResolver): DynamicAppliedType? = resolver[info]

            override fun dynamicApply(typeArgs: List<ApplicationParameter>): Dynamic? {
                return Indirect(info = info.dynamicApply(typeArgs) ?: return null)
            }

            override fun indirect(): Indirect = this

        }

        data class Direct(
                override val type: DynamicAppliedType
        ) : Dynamic(), DirectHolder<CompleteMinInfo.Dynamic, DynamicAppliedType> {

            override val info: CompleteMinInfo.Dynamic
                get() = type.completeInfo

            override fun with(resolver: TypeResolver): DynamicAppliedType? = type

            override fun staticApply(typeArgs: List<Static>): Static? {
                val argTypes = typeArgs.castIfAllInstance<Static.Direct>()
                return if (argTypes != null) {
                    Static.Direct(type.staticApply(argTypes) ?: return null)
                } else {
                    super.staticApply(typeArgs)
                }
            }

            override fun dynamicApply(typeArgs: List<ApplicationParameter>): Dynamic? {
                return Direct(type.dynamicApply(typeArgs) ?: return null)
            }

            override fun applySelf(self: Static): Dynamic {
                return if (self is Static.Direct) {
                    Direct(type.applySelf(self))
                } else {
                    super.applySelf(self)
                }
            }

            override fun indirect(): Indirect = Indirect(info)

            override fun toString() = "!$info"

        }

        override fun staticApply(typeArgs: List<Static>): Static? {
            return info.staticApply(typeArgs) ?: return null
        }

        override fun applySelf(self: Static): Dynamic = Indirect(info = info.applySelf(self))

        abstract override fun indirect(): Dynamic

    }

    sealed class Static : TypeHolder<CompleteMinInfo.Static, NonGenericType>() {

        data class Indirect(
                override val info: CompleteMinInfo.Static
        ) : Static() {

            override fun with(resolver: TypeResolver): NonGenericType? = resolver[info]

            override fun indirect(): Static = this

        }

        data class Direct(
                override val type: NonGenericType
        ) : Static(), DirectHolder<CompleteMinInfo.Static, NonGenericType> {

            override val info: CompleteMinInfo.Static
                get() = type.completeInfo

            override fun with(resolver: TypeResolver): NonGenericType? = type

            override fun indirect(): Indirect = Indirect(info)

            override fun toString() = "!$info"

        }

        override fun staticApply(typeArgs: List<Static>): Static = this

        override fun applySelf(self: Static): TypeHolder<*, *> = this

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): TypeHolder<*, *> = this

        abstract override fun indirect(): Static

        override fun toString() = info.toString()

    }

    abstract fun indirect(): TypeHolder<I, T>

    abstract fun with(resolver: TypeResolver): T?

    override fun toString() = info.toString()

}