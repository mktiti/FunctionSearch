package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.BoundedWildcard
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.SamType.DirectSam
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.util.castIfAllInstance
import com.mktiti.fsearch.core.util.genericString
import java.util.*

interface SemiType {

    // val info: TypeInfo
    val info: MinimalInfo
    val virtual: Boolean
    val superTypes: List<TypeHolder<*, *>>

    val samType: SamType<*>?

    val fullName: String

    /*
    val supersTree: TreeNode<SemiType>
        get() = TreeNode(this, superTypes.map { it.type.supersTree })


    fun anySuper(predicate: (Type) -> Boolean): Boolean = superTypes.asSequence().any { s ->
        s.type.let {
            predicate(it) || it.anySuper(predicate)
        }
    }
*/
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

          //  override val typeParamString: String
            //    get() = ""

        }

        class StaticAppliedType(
                base: MinimalInfo,
                private val baseSam: SamType.GenericSam?,
                override val typeArgs: List<TypeHolder.Static>,
                override val superTypes: List<TypeHolder.Static>,
                virtual: Boolean = false
        ) : NonGenericType(base.staticComplete(typeArgs.map { it.info }), virtual) {

            // override val typeArgs: List<TypeHolder.Static>
             //   get() = completeInfo.args

            // override val superTypes: List<CompleteMinInfo.Static> =
            // baseType.superTypes.map { it.staticApply(typeArgs) }.liftNull() ?:
            // throw TypeApplicationException("Failed to static apply type $baseType with $typeArgs")

            override val samType: DirectSam? by lazy {
                baseSam?.staticApply(typeArgs)
            }
        }

        override fun holder(): TypeHolder.Static = TypeHolder.Static.Direct(this)
/*
            override val typeParamString by lazy {
                // type.typeParams.zip(typeArgs).genericString { (_, arg) -> arg.fullName }
                "TODO"
            }

        }

        private fun anyNgSuper(predicate: (NonGenericType) -> Boolean): Boolean = superTypes.asSequence().any { s ->
            s.type.anyNgSuperInclusive(predicate)
        }

        fun anyNgSuperInclusive(predicate: (NonGenericType) -> Boolean): Boolean = predicate(this) || anyNgSuper(predicate)
*/
    }

    data class DynamicAppliedType(
            val base: MinimalInfo,
            private val baseSam: SamType.GenericSam?,
            val typeArgMapping: List<ApplicationParameter>,
            // override val completeInfo: CompleteMinInfo.Dynamic,
            override val superTypes: List<TypeHolder<*, *>>,
            override val virtual: Boolean = false
    ) : Type<CompleteMinInfo.Dynamic>(), TypeApplicable {

        override val completeInfo: CompleteMinInfo.Dynamic
            get() = base.dynamicComplete(typeArgMapping)

        override val info: MinimalInfo
            get() = base

        // val typeArgMapping
        //    get() = completeInfo.args

        override val samType: SamType.GenericSam? by lazy {
            baseSam?.dynamicApply(typeArgMapping)
        }
/*
        override val typeParamString by lazy {
            //type..zip(typeArgMapping).genericString { (_, arg) -> arg.toString() }
            // TODO
            "<args todo>"
        }

        override val superTypes: List<SuperType<Type>> =
                baseType.superTypes.map { it.dynamicApply(typeArgMapping) }.liftNull() ?:
                    throw TypeApplicationException("Failed to dynamically apply type $baseType with $typeArgMapping")
 */

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
                    // completeInfo = completeInfo.applySelf(self),
                    typeArgMapping = typeArgMapping.map { it.applySelf(self) },
                    superTypes = superTypes.map { it.applySelf(self) }
            )
        }

/*
        override fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
            val mappedArgs: List<NonGenericType> = typeArgMapping.map { argMapping ->
                when (argMapping) {
                    is Substitution -> argMapping.staticApply(typeArgs) ?: return null
                    is Wildcard.Direct -> NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), samType = null) // TODO
                    is Wildcard.Bounded -> return null
                }
            }

            return StaticAppliedType(
                baseType = baseType,
                typeArgs = mappedArgs
            )
        }
 */
/*
        // TODO
        fun applySelf(self: CompleteMinInfo.Static): DynamicAppliedType {
            /*val appliedArgs: List<ApplicationParameter> = typeArgMapping.map { it.applySelf(self) }

            return copy(
                typeArgMapping = appliedArgs
            )
             */

            return copy(
                    completeInfo = completeInfo.applySelf(self)
            )
        }

 */
        override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
            return copy(
                    // completeInfo = completeInfo.dynamicApply(typeArgs) ?: return null,
                    typeArgMapping = typeArgMapping.map { it.dynamicApply(typeArgs) ?: return null },
                    superTypes = superTypes.map { it.dynamicApply(typeArgs) ?: return null },
                    virtual = virtual
            )
        }

        override fun holder(): TypeHolder.Dynamic = TypeHolder.Dynamic.Direct(this)

    }

    // open fun anySuperInclusive(predicate: (Type) -> Boolean): Boolean = predicate(this) || anySuper(predicate)

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
            //completeInfo = info.dynamicComplete(typeArgs),
            base = info,
            baseSam = samType,
            typeArgMapping = typeArgs,
            superTypes = superTypes.map { it.dynamicApply(typeArgs) ?: return null },
            virtual = virtual
        )
    }

    override fun toString() = fullName
}