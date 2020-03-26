import ApplicationParameter.ParamSubstitution
import ApplicationParameter.TypeSubstitution
import ApplicationParameter.TypeSubstitution.DynamicTypeSubstitution
import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import SuperType.DynamicSuper
import SuperType.StaticSuper
import Type.DynamicAppliedType
import Type.NonGenericType
import Type.NonGenericType.StaticAppliedType
import java.util.*

sealed class ApplicationParameter {

    class ParamSubstitution(val param: Int) : ApplicationParameter() {
        override fun toString() = "<#$param>"
    }

    sealed class TypeSubstitution<T : Type>(
        val type: T
    ) : ApplicationParameter() {

        companion object {
            fun wrap(type: Type): TypeSubstitution<*> = when (type) {
                is NonGenericType -> StaticTypeSubstitution(type)
                is DynamicAppliedType -> DynamicTypeSubstitution(type)
            }
        }

        class DynamicTypeSubstitution(type: DynamicAppliedType) : TypeSubstitution<DynamicAppliedType>(type)

        class StaticTypeSubstitution(type: NonGenericType) : TypeSubstitution<NonGenericType>(type)

        override fun toString() = type.fullName

    }

}

sealed class SuperType {

    abstract val type: Type

    class StaticSuper(
        override val type: NonGenericType
    ) : SuperType()

    class DynamicSuper(
        override val type: DynamicAppliedType
    ) : SuperType()

}

interface SemiType {

    val info: TypeInfo
    val superTypes: List<SuperType>

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

    fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType?

    fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType?

    fun apply(typeArgs: List<ApplicationParameter>): Type? {
        val nonGenericArgs: List<StaticTypeSubstitution>? = typeArgs.castIfAllInstance()

        return if (nonGenericArgs == null) {
            dynamicApply(typeArgs)
        } else {
            staticApply(nonGenericArgs.map { it.type })
        }
    }

}

sealed class Type : SemiType {

    sealed class NonGenericType(
        override val info: TypeInfo,
        override val superTypes: List<StaticSuper>
    ) : Type() {

        abstract val typeArgs: List<NonGenericType>

        class DirectType(
            info: TypeInfo,
            superTypes: List<StaticSuper>
        ) : NonGenericType(info, superTypes) {

            override val typeArgs: List<NonGenericType>
                get() = emptyList()

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
            val baseType: TypeTemplate,
            override val typeArgs: List<NonGenericType>,
            superTypes: List<StaticSuper>
        ) : NonGenericType(baseType.info, superTypes) {

            override val typeParamString = baseType.typeParams.zip(typeArgs).genericString { (_, arg) -> arg.fullName }

        }

        fun anyNgSuper(predicate: (NonGenericType) -> Boolean): Boolean = superTypes.asSequence().any { s ->
            s.type.let {
                predicate(it) || it.anyNgSuper(predicate)
            }
        }

        fun anyNgSuperInclusive(predicate: (NonGenericType) -> Boolean): Boolean = predicate(this) || anyNgSuper(predicate)

    }

    class DynamicAppliedType(
        val baseType: TypeTemplate,
        val typeArgMapping: List<ApplicationParameter>,
        override val superTypes: List<SuperType>
    ) : Type(), Applicable {

        override val info: TypeInfo
            get() = baseType.info

        override val typeParamString = baseType.typeParams.zip(typeArgMapping).genericString { (_, arg) ->
            when (arg) {
                is ParamSubstitution -> "#${arg.param}"
                is TypeSubstitution<*> -> arg.type.fullName
            }
        }

        override fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
            val mappedArgs: List<NonGenericType> = typeArgMapping.map { argMapping ->
                when (argMapping) {
                    is ParamSubstitution -> typeArgs.getOrNull(argMapping.param) ?: return null
                    is DynamicTypeSubstitution -> argMapping.type.staticApply(typeArgs) ?: return null
                    is StaticTypeSubstitution -> argMapping.type
                }
            }

            val appliedSupers = superTypes.map { superType ->
                when (superType) {
                    is StaticSuper -> superType
                    is DynamicSuper -> StaticSuper(superType.type.staticApply(typeArgs) ?: return null)
                }
            }

            return StaticAppliedType(
                baseType = baseType,
                typeArgs = mappedArgs,
                superTypes = appliedSupers
            )
        }

        override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
            val mappedArgs: List<ApplicationParameter> = typeArgMapping.map { argMapping ->
                when (argMapping) {
                    is ParamSubstitution -> typeArgs.getOrNull(argMapping.param) ?: return null
                    is DynamicTypeSubstitution -> {
                        DynamicTypeSubstitution(argMapping.type.dynamicApply(typeArgs) ?: return null)
                    }
                    is StaticTypeSubstitution -> argMapping
                }
            }

            val appliedSupers = superTypes.map { superType ->
                when (superType) {
                    is StaticSuper -> superType
                    is DynamicSuper -> DynamicSuper(superType.type.dynamicApply(typeArgs) ?: return null)
                }
            }

            return DynamicAppliedType(
                baseType = baseType,
                typeArgMapping = mappedArgs,
                superTypes = appliedSupers
            )
        }

    }

    open fun anySuperInclusive(predicate: (Type) -> Boolean): Boolean = predicate(this) || anySuper(predicate)

    override fun equals(other: Any?) = (other as? Type)?.info == info

    override fun hashCode(): Int = Objects.hashCode(info)

}

class TypeTemplate(
    override val info: TypeInfo,
    val typeParams: List<TypeParameter>,
    override val superTypes: List<SuperType>
) : Applicable, SemiType {

    override val typeParamString: String
        get() = typeParams.genericString { it.toString() }

    private fun <A : Any, S : SuperType> applyBase(
        typeArgs: List<A>,
        staticMap: (StaticSuper) -> S,
        superApplier: (DynamicSuper) -> S?
    ): List<S>? {
        if (typeParams.size != typeArgs.size) {
            return null
        }

        return superTypes.map { superType ->
            when (superType) {
                is StaticSuper -> staticMap(superType)
                is DynamicSuper -> superApplier(superType) ?: return null
            }
        }
    }

    override fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType? {
        return StaticAppliedType(
            baseType = this,
            typeArgs = typeArgs,
            superTypes = applyBase(typeArgs, ::identity) {
                it.type.staticApply(typeArgs)?.let(SuperType::StaticSuper)
            } ?: return null
        )
    }

    override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
        return DynamicAppliedType(
            baseType = this,
            typeArgMapping = typeArgs,
            superTypes = applyBase(typeArgs, ::identity) {
                it.type.dynamicApply(typeArgs)?.let(SuperType::DynamicSuper)
            } ?: return null
        )
    }
}