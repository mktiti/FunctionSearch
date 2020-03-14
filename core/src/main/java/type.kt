import ApplicationParameter.*
import SuperType.DynamicSuper
import SuperType.StaticSuper
import Type.GenericType
import Type.GenericType.DynamicAppliedType
import Type.GenericType.TypeTemplate
import Type.NonGenericType
import Type.NonGenericType.DirectType
import Type.NonGenericType.StaticAppliedType

sealed class ApplicationParameter {

    class ParamSubstitution(val param: Int) : ApplicationParameter() {
        override fun toString() = "<#$param>"
    }

    class DynamicTypeSubstitution(val type: DynamicAppliedType) : ApplicationParameter() {
        override fun toString() = type.fullName
    }

    class StaticTypeSubstitution(val type: NonGenericType) : ApplicationParameter() {
        override fun toString() = type.fullName
    }

}

class TypeParameter(
    val sign: String,
    val lowerBounds: List<Type> = emptyList(), // super
    val upperBounds: List<Type> = emptyList()  // extends
) {

    override fun toString() = buildString {
        append(sign)
        if (upperBounds.isNotEmpty()) {
            val upperString = upperBounds.joinToString(prefix = " extends ", separator = " & ") { it.fullName }
            append(upperString)
        }
        if (lowerBounds.isNotEmpty()) {
            lowerBounds.joinToString(prefix = " super ", separator = " & ") { it.fullName }
            append(lowerBounds)
        }
    }

}

sealed class SuperType {

    abstract val type: Type

    class StaticSuper(
        override val type: NonGenericType
    ) : SuperType()

    class DynamicSuper(
        override val type: GenericType
    ) : SuperType()

}

sealed class Type {

    abstract val info: TypeInfo
    abstract val superTypes: List<SuperType>

    abstract val typeParamString: String
    val fullName by lazy {
        buildString {
            append(info)
            append(typeParamString)
        }
    }

    val supersTree: TreeNode<Type> by lazy {
        TreeNode(this, superTypes.map { it.type.supersTree })
    }

    sealed class NonGenericType(
        override val info: TypeInfo,
        override val superTypes: List<StaticSuper>
    ) : Type() {

        class DirectType(
            info: TypeInfo,
            superTypes: List<StaticSuper>
        ) : NonGenericType(info, superTypes) {

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
            val baseType: TypeTemplate,
            val typeArgs: List<NonGenericType>,
            superTypes: List<StaticSuper>
        ) : NonGenericType(baseType.info, superTypes) {

            override val typeParamString = baseType.typeParams.zip(typeArgs).genericString { (param, arg) -> "${param.sign} = ${arg.fullName}" }

        }

    }

    sealed class GenericType(
        override val superTypes: List<SuperType>
    ) : Type() {

        class TypeTemplate(
            override val info: TypeInfo,
            val typeParams: List<TypeParameter>,
            superTypes: List<SuperType>
        ) : GenericType(superTypes) {

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

        class DynamicAppliedType(
            val baseType: TypeTemplate,
            val typeArgMapping: List<ApplicationParameter>,
            superTypes: List<SuperType>
        ) : GenericType(superTypes) {

            override val info: TypeInfo
                get() = baseType.info

            override val typeParamString = baseType.typeParams.zip(typeArgMapping).genericString { (param, arg) ->
                "${param.sign} = " + when (arg) {
                    is ParamSubstitution -> "#${arg.param}"
                    is DynamicTypeSubstitution -> arg.type.fullName
                    is StaticTypeSubstitution -> arg.type.fullName
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

        abstract fun staticApply(typeArgs: List<NonGenericType>): StaticAppliedType?

        abstract fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType?

        fun apply(typeArgs: List<ApplicationParameter>): Type? {
            val nonGenericArgs: List<StaticTypeSubstitution>? = typeArgs.castIfAllInstance()
            
            return if (nonGenericArgs == null) {
                dynamicApply(typeArgs)
            } else {
                staticApply(nonGenericArgs.map { it.type })
            }
        }

        fun staticApply(vararg typeArgs: NonGenericType): StaticAppliedType? = staticApply(typeArgs.toList())

        fun forceStaticApply(typeArgs: List<NonGenericType>): StaticAppliedType
                = staticApply(typeArgs) ?: throw TypeApplicationException("Failed to static apply type args $typeArgs to $info")

        fun forceStaticApply(vararg typeArgs: NonGenericType): StaticAppliedType
                = forceStaticApply(typeArgs.toList())

        fun dynamicApply(vararg typeArgs: ApplicationParameter): DynamicAppliedType? = dynamicApply(typeArgs.toList())

        fun forceDynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType
                = dynamicApply(typeArgs) ?: throw TypeApplicationException("Failed to dynamically apply type args $typeArgs to $info")

        fun forceDynamicApply(vararg typeArgs: ApplicationParameter): DynamicAppliedType
                = forceDynamicApply(typeArgs.toList())

        fun forceApply(typeArgs: List<ApplicationParameter>): Type
                = apply(typeArgs) ?: throw TypeApplicationException("Failed to apply type args to $info")

        fun forceApply(vararg typeArgs: ApplicationParameter): Type = forceApply(typeArgs.toList())

    }

}

fun directType(fullName: String, vararg superType: NonGenericType)
        = DirectType(info(fullName), superType.map { StaticSuper(it) })

fun typeTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type>) = TypeTemplate(
    info(fullName), typeParams, superTypes.map {
        when (it) {
            is NonGenericType -> StaticSuper(it)
            is GenericType -> DynamicSuper(it)
        }
    }
)
