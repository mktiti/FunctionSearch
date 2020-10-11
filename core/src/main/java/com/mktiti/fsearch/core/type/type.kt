package com.mktiti.fsearch.core.type

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.ApplicationParameter.Wildcard
import com.mktiti.fsearch.core.type.SamType.DirectSam
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.StaticAppliedType
import com.mktiti.fsearch.core.util.*
import java.util.*

interface SemiType {

    // val info: TypeInfo
    val info: MinimalInfo
    val virtual: Boolean
    val superTypes: List<CompleteMinInfo<*>>

    val samType: SamType<Substitution>?

    val typeParamString: String
    val fullName: String
        get() = buildString {
            append(info)
            append(typeParamString)
        }

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

interface Applicable {

    val staticApplicable: Boolean

    fun staticApplyInfo(typeArgs: List<CompleteMinInfo.Static>): StaticAppliedType?

    fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
        return staticApplyInfo(typeArgs.map(NonGenericType::completeInfo))
    }

    fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType?

    fun apply(typeArgs: List<ApplicationParameter>): Type? {
        val argsAsStatic = typeArgs.map {
            if (it is Wildcard.Direct) {
                // StaticTypeSubstitution(NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), null))
                null
            } else {
                it
            }
        }.castIfAllInstance<StaticTypeSubstitution>()

        return if (argsAsStatic == null) {
            dynamicApply(typeArgs)
        } else {
            if (staticApplicable) {
                staticApplyInfo(argsAsStatic.map(StaticTypeSubstitution::type))
            } else {
                dynamicApply(typeArgs)
            }
        }
    }

}

sealed class Type : SemiType {

    sealed class NonGenericType(
            val completeInfo: CompleteMinInfo.Static,
            override val virtual: Boolean
    ) : Type() {

        override val info: MinimalInfo
            get() = completeInfo.base

        abstract override val superTypes: List<CompleteMinInfo.Static>

        abstract override val samType: DirectSam?

        abstract val typeArgs: List<CompleteMinInfo.Static>

        class DirectType(
                minInfo: MinimalInfo,
                virtual: Boolean,
                override val superTypes: List<CompleteMinInfo.Static>,
                override val samType: DirectSam?
        ) : NonGenericType(minInfo.complete(), virtual) {

            override val typeArgs: List<CompleteMinInfo.Static>
                get() = emptyList()

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
                type: CompleteMinInfo.Static,
                override val superTypes: List<CompleteMinInfo.Static>,
                virtual: Boolean = false
        ) : NonGenericType(type, virtual) {

            override val typeArgs: List<CompleteMinInfo.Static>
                get() = completeInfo.args

            // override val superTypes: List<CompleteMinInfo.Static> =
                // baseType.superTypes.map { it.staticApply(typeArgs) }.liftNull() ?:
                   // throw TypeApplicationException("Failed to static apply type $baseType with $typeArgs")

            override val samType: DirectSam? = null

            override val typeParamString by lazy {
                // type.typeParams.zip(typeArgs).genericString { (_, arg) -> arg.fullName }
                "TODO"
            }

        }
/*
        private fun anyNgSuper(predicate: (NonGenericType) -> Boolean): Boolean = superTypes.asSequence().any { s ->
            s.type.anyNgSuperInclusive(predicate)
        }

        fun anyNgSuperInclusive(predicate: (NonGenericType) -> Boolean): Boolean = predicate(this) || anyNgSuper(predicate)
*/
    }

    data class DynamicAppliedType(
            val completeInfo: CompleteMinInfo.Dynamic,
            override val superTypes: List<CompleteMinInfo<*>>,
            override val virtual: Boolean = false
    ) : Type(), Applicable {

        override val info: MinimalInfo
            get() = completeInfo.base

        val typeArgMapping
            get() = completeInfo.args

        override val samType: SamType<Substitution>? by lazy {
            //ty.samType?.dynamicApply(typeArgMapping)
            null
        }

        override val typeParamString by lazy {
            //type..zip(typeArgMapping).genericString { (_, arg) -> arg.toString() }
            // TODO
            "<args todo>"
        }
/*
        override val superTypes: List<SuperType<Type>> =
                baseType.superTypes.map { it.dynamicApply(typeArgMapping) }.liftNull() ?:
                    throw TypeApplicationException("Failed to dynamically apply type $baseType with $typeArgMapping")
 */

        override val staticApplicable: Boolean
            get() = typeArgMapping.filterIsInstance<Wildcard.Bounded>().none()

        override fun staticApplyInfo(typeArgs: List<CompleteMinInfo.Static>): StaticAppliedType? {
            /*val mappedArgs: List<NonGenericType> = typeArgMapping.map { argMapping ->
                when (argMapping) {
                    is Substitution -> argMapping.staticApply(typeArgs) ?: return null
                    is Wildcard.Direct -> NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), samType = null) // TODO
                    is Wildcard.Bounded -> return null
                }
            }
             */

            return StaticAppliedType(
                    type = completeInfo.staticApply(typeArgs) ?: return null,
                    superTypes = superTypes.map {
                        when (it) {
                            is CompleteMinInfo.Static -> it
                            is CompleteMinInfo.Dynamic -> it.staticApply(typeArgs) ?: return null
                        }
                    },
                    virtual = virtual
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

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
            /*val mappedArgs: List<ApplicationParameter> = typeArgMapping.map { it.dynamicApply(typeArgs) ?: return null }

            return DynamicAppliedType(
                baseType = baseType,
                typeArgMapping = mappedArgs
            )

             */

            val type = info.dynamicComplete(typeArgs)

            return DynamicAppliedType(
                    completeInfo = type,
                    superTypes = superTypes.map {
                        when (it) {
                            is CompleteMinInfo.Static -> it
                            is CompleteMinInfo.Dynamic -> it.dynamicApply(typeArgs) ?: return null
                        }
                    },
                    virtual = virtual
            )
        }

    }

    // open fun anySuperInclusive(predicate: (Type) -> Boolean): Boolean = predicate(this) || anySuper(predicate)

    override fun equals(other: Any?) = (other as? Type)?.info == info

    override fun hashCode(): Int = Objects.hashCode(info)

    override fun toString() = fullName

}

class TypeTemplate(
        override val info: MinimalInfo,
        val typeParams: List<TypeParameter>,
        override val superTypes: List<CompleteMinInfo<*>>,
        override val samType: SamType<Substitution>?,
        override val virtual: Boolean = false
) : Applicable, SemiType {

    override val typeParamString: String
        get() = typeParams.genericString { it.toString() }

    override val staticApplicable: Boolean
        get() = true

    override fun staticApplyInfo(typeArgs: List<CompleteMinInfo.Static>): StaticAppliedType? {
        return StaticAppliedType(
                type = info.staticComplete(typeArgs),
                superTypes = superTypes.map { it.staticApply(typeArgs) ?: return null },
                virtual = virtual
        )
    }

    override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
        val type = info.dynamicComplete(typeArgs)

        return DynamicAppliedType(
            completeInfo = type,
            superTypes = superTypes.map {
                when (it) {
                    is CompleteMinInfo.Static -> it
                    is CompleteMinInfo.Dynamic -> it.dynamicApply(typeArgs) ?: return null
                }
            },
            virtual = virtual
        )
    }

    override fun toString() = fullName
}