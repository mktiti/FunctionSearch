package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.SamType.DirectSam
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.util.castIfAllInstance
import com.mktiti.fsearch.core.util.genericString
import java.util.*

interface SemiType {

    val info: MinimalInfo
    val virtual: Boolean
    val superTypes: List<TypeHolder<*, *>>

    val samType: SamType<*>?

    val fullName: String

}

sealed class Type<out I : CompleteMinInfo<*>> : SemiType {

    sealed class NonGenericType(
            override val completeInfo: CompleteMinInfo.Static,
            override val virtual: Boolean
    ) : Type<CompleteMinInfo.Static>() {

        override val info: MinimalInfo
            get() = completeInfo.base

        abstract override val superTypes: List<TypeHolder.Static>

        abstract override val samType: DirectSam?

        abstract val typeArgs: List<TypeHolder.Static>

        class DirectType(
                minInfo: MinimalInfo,
                virtual: Boolean,
                override val superTypes: List<TypeHolder.Static>,
                override val samType: DirectSam?
        ) : NonGenericType(minInfo.complete(), virtual) {

            override val typeArgs: List<TypeHolder.Static>
                get() = emptyList()

        }

        class StaticAppliedType(
                base: MinimalInfo,
                private val baseSam: SamType.GenericSam?,
                override val typeArgs: List<TypeHolder.Static>,
                override val superTypes: List<TypeHolder.Static>,
                virtual: Boolean = false
        ) : NonGenericType(base.staticComplete(typeArgs.map { it.info }), virtual) {

            override val samType: DirectSam? by lazy {
                baseSam?.staticApply(typeArgs)
            }
        }

        override fun holder(): TypeHolder.Static = TypeHolder.Static.Direct(this)

    }

    data class DynamicAppliedType(
            val base: MinimalInfo,
            private val baseSam: SamType.GenericSam?,
            val typeArgMapping: List<ApplicationParameter>,
            override val superTypes: List<TypeHolder<*, *>>,
            override val virtual: Boolean = false
    ) : Type<CompleteMinInfo.Dynamic>(), TypeApplicable {

        override val completeInfo: CompleteMinInfo.Dynamic
            get() = base.dynamicComplete(typeArgMapping)

        override val info: MinimalInfo
            get() = base

        override val samType: SamType.GenericSam? by lazy {
            baseSam?.dynamicApply(typeArgMapping)
        }

        override val staticApplicable: Boolean
            get() = typeArgMapping.filterIsInstance<BoundedWildcard>().none()

        override fun staticApply(typeArgs: List<TypeHolder.Static>): StaticAppliedType? {
            return typeArgMapping.castIfAllInstance<Substitution>()?.let { subArgs ->
                return StaticAppliedType(
                        base = info,
                        baseSam = baseSam,
                        typeArgs = subArgs.map { it.staticApply(typeArgs) ?: return null },
                        superTypes = superTypes.map { it.staticApply(typeArgs) ?: return null },
                        virtual = virtual
                )
            }
        }

        fun applySelf(self: TypeHolder.Static): DynamicAppliedType {
            return copy(
                    typeArgMapping = typeArgMapping.map { it.applySelf(self) },
                    superTypes = superTypes.map { it.applySelf(self) }
            )
        }

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
            return copy(
                    typeArgMapping = typeArgMapping.map { it.dynamicApply(typeArgs) ?: return null },
                    superTypes = superTypes.map { it.dynamicApply(typeArgs) ?: return null },
                    virtual = virtual
            )
        }

        override fun holder(): TypeHolder.Dynamic = TypeHolder.Dynamic.Direct(this)

    }

    abstract val completeInfo: I

    abstract override val superTypes: List<TypeHolder<*, *>>

    override val fullName by lazy {
        completeInfo.toString()
    }

    abstract fun holder(): TypeHolder<I, *>

    override fun equals(other: Any?) = (other as? Type<*>)?.info == info

    override fun hashCode(): Int = Objects.hashCode(info)

    override fun toString() = fullName

}

class TypeTemplate(
        override val info: MinimalInfo,
        val typeParams: List<TypeParameter>,
        override val superTypes: List<TypeHolder<*, *>>,
        override val samType: SamType.GenericSam?,
        override val virtual: Boolean = false
) : TypeApplicable, SemiType {

    override val fullName by lazy {
        buildString {
            append(info)
            append(typeParams.genericString())
        }
    }

    override val staticApplicable: Boolean
        get() = true

    override fun staticApply(typeArgs: List<TypeHolder.Static>): StaticAppliedType? {
        return StaticAppliedType(
                base = info,
                baseSam = samType,
                typeArgs = typeArgs,
                superTypes = superTypes.map { it.staticApply(typeArgs) ?: return null },
                virtual = virtual
        )
    }

    override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
        return DynamicAppliedType(
            base = info,
            baseSam = samType,
            typeArgMapping = typeArgs,
            superTypes = superTypes.map { it.dynamicApply(typeArgs) ?: return null },
            virtual = virtual
        )
    }

    override fun toString() = fullName
}