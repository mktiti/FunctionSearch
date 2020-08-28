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

    val info: TypeInfo
    val superTypes: List<SuperType<Type>>

    val samType: SamType<Substitution>?

    val typeParamString: String
    val fullName: String
        get() = buildString {
            append(info)
            append(typeParamString)
        }

    val supersTree: TreeNode<SemiType>
        get() = TreeNode(this, superTypes.map { it.type.supersTree })

    fun anySuper(predicate: (Type) -> Boolean): Boolean = superTypes.asSequence().any { s ->
        s.type.let {
            predicate(it) || it.anySuper(predicate)
        }
    }

}

interface Applicable {

    val canByStaticApplied: Boolean

    fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType?

    fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType?

    fun apply(typeArgs: List<ApplicationParameter>): Type? {
        val argsAsStatic = typeArgs.map {
            if (it is Wildcard.Direct) {
                StaticTypeSubstitution(NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), null))
            } else {
                it
            }
        }.castIfAllInstance<StaticTypeSubstitution>()

        return if (argsAsStatic == null) {
            dynamicApply(typeArgs)
        } else {
            if (canByStaticApplied) {
                staticApply(argsAsStatic.map(StaticTypeSubstitution::type))
            } else {
                dynamicApply(typeArgs)
            }
        }
    }

}

sealed class Type : SemiType {

    sealed class NonGenericType(
        override val info: TypeInfo
    ) : Type() {

        abstract override val superTypes: List<SuperType<NonGenericType>>

        abstract override val samType: DirectSam?

        abstract val typeArgs: List<NonGenericType>

        class DirectType(
                info: TypeInfo,
                override val superTypes: List<SuperType<NonGenericType>>,
                override val samType: DirectSam?
        ) : NonGenericType(info) {

            override val typeArgs: List<NonGenericType>
                get() = emptyList()

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
                val baseType: TypeTemplate,
                override val typeArgs: List<NonGenericType>
        ) : NonGenericType(baseType.info) {

            override val superTypes: List<SuperType<NonGenericType>> =
                baseType.superTypes.map { it.staticApply(typeArgs) }.liftNull() ?:
                    throw TypeApplicationException("Failed to static apply type $baseType with $typeArgs")

            override val samType: DirectSam? by lazy {
                baseType.samType?.staticApply(typeArgs)
            }

            override val typeParamString by lazy {
                baseType.typeParams.zip(typeArgs).genericString { (_, arg) -> arg.fullName }
            }

        }

        private fun anyNgSuper(predicate: (NonGenericType) -> Boolean): Boolean = superTypes.asSequence().any { s ->
            s.type.anyNgSuperInclusive(predicate)
        }

        fun anyNgSuperInclusive(predicate: (NonGenericType) -> Boolean): Boolean = predicate(this) || anyNgSuper(predicate)

    }

    data class DynamicAppliedType(
            val baseType: TypeTemplate,
            val typeArgMapping: List<ApplicationParameter>
    ) : Type(), Applicable {

        override val info: TypeInfo
            get() = baseType.info

        override val samType: SamType<Substitution>? by lazy {
            baseType.samType?.dynamicApply(typeArgMapping)
        }

        override val typeParamString by lazy {
            baseType.typeParams.zip(typeArgMapping).genericString { (_, arg) -> arg.toString() }
        }

        override val superTypes: List<SuperType<Type>> =
                baseType.superTypes.map { it.dynamicApply(typeArgMapping) }.liftNull() ?:
                    throw TypeApplicationException("Failed to dynamically apply type $baseType with $typeArgMapping")

        override val canByStaticApplied: Boolean
            get() = typeArgMapping.filterIsInstance<Wildcard.BoundedWildcard>().none()

        override fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
            val mappedArgs: List<NonGenericType> = typeArgMapping.map { argMapping ->
                when (argMapping) {
                    is Substitution -> argMapping.staticApply(typeArgs) ?: return null
                    is Wildcard.Direct -> NonGenericType.DirectType(TypeInfo.anyWildcard, emptyList(), samType = null) // TODO
                    is Wildcard.BoundedWildcard -> return null
                }
            }

            return StaticAppliedType(
                baseType = baseType,
                typeArgs = mappedArgs
            )
        }

        // TODO
        fun applySelf(self: NonGenericType): DynamicAppliedType {
            val appliedArgs: List<ApplicationParameter> = typeArgMapping.map { it.applySelf(self) }

            return copy(
                typeArgMapping = appliedArgs
            )
        }

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
            val mappedArgs: List<ApplicationParameter> = typeArgMapping.map { it.dynamicApply(typeArgs) ?: return null }

            return DynamicAppliedType(
                baseType = baseType,
                typeArgMapping = mappedArgs
            )
        }

    }

    open fun anySuperInclusive(predicate: (Type) -> Boolean): Boolean = predicate(this) || anySuper(predicate)

    override fun equals(other: Any?) = (other as? Type)?.info == info

    override fun hashCode(): Int = Objects.hashCode(info)

    override fun toString() = fullName

}

class TypeTemplate(
        override val info: TypeInfo,
        val typeParams: List<TypeParameter>,
        override val superTypes: List<SuperType<Type>>,
        override val samType: SamType<Substitution>?
) : Applicable, SemiType {

    override val typeParamString: String
        get() = typeParams.genericString { it.toString() }

    override val canByStaticApplied: Boolean
        get() = true

    override fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
        return StaticAppliedType(
            baseType = this,
            typeArgs = typeArgs
        )
    }

    override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
        return DynamicAppliedType(
            baseType = this,
            typeArgMapping = typeArgs
        )
    }

    override fun toString() = fullName
}