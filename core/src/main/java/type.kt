sealed class ApplicationParameter {

    class ParamSubstitution(val param: Int) : ApplicationParameter() {
        override fun toString() = "<#$param>"
    }

    class DynamicTypeSubstitution(val type: Type.GenericType.DynamicAppliedType) : ApplicationParameter() {
        override fun toString() = type.fullName
    }

    class StaticTypeSubstitution(val type: Type.NonGenericType) : ApplicationParameter() {
        override fun toString() = type.fullName
    }

}

sealed class SuperType {

    abstract val type: Type

    class StaticSuper(
        override val type: Type.NonGenericType
    ) : SuperType()

    class DynamicSuper(
        override val type: Type.GenericType
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
        override val superTypes: List<SuperType.StaticSuper>
    ) : Type() {

        class DirectType(
            info: TypeInfo,
            superTypes: List<SuperType.StaticSuper>
        ) : NonGenericType(info, superTypes) {

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
            val baseType: GenericType.TypeTemplate,
            val typeArgs: List<NonGenericType>,
            superTypes: List<SuperType.StaticSuper>
        ) : NonGenericType(baseType.info, superTypes) {

            override val typeParamString = baseType.typeParams.zip(typeArgs).genericString { (param, arg) -> "$param = ${arg.fullName}" }

        }

    }

    sealed class GenericType(
        override val superTypes: List<SuperType>
    ) : Type() {

        class TypeTemplate(
            override val info: TypeInfo,
            val typeParams: List<String>,
            superTypes: List<SuperType>
        ) : GenericType(superTypes) {

            override val typeParamString: String
                get() = typeParams.genericString { it }

            private fun <A : Any, S : SuperType> applyBase(
                typeArgs: List<A>,
                staticMap: (SuperType.StaticSuper) -> S,
                superApplier: (SuperType.DynamicSuper) -> S?
            ): List<S>? {
                if (typeParams.size != typeArgs.size) {
                    return null
                }

                return superTypes.map { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> staticMap(superType)
                        is SuperType.DynamicSuper -> superApplier(superType) ?: return null
                    }
                }
            }

            override fun staticApply(typeArgs: List<NonGenericType>): NonGenericType.StaticAppliedType? {
                return NonGenericType.StaticAppliedType(
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
                "$param = " + when (arg) {
                    is ApplicationParameter.ParamSubstitution -> "#${arg.param}"
                    is ApplicationParameter.DynamicTypeSubstitution -> arg.type.fullName
                    is ApplicationParameter.StaticTypeSubstitution -> arg.type.fullName
                }
            }

            override fun staticApply(typeArgs: List<NonGenericType>): NonGenericType.StaticAppliedType? {
               val mappedArgs: List<NonGenericType> = typeArgMapping.map { argMapping ->
                   when (argMapping) {
                       is ApplicationParameter.ParamSubstitution -> typeArgs.getOrNull(argMapping.param) ?: return null
                       is ApplicationParameter.DynamicTypeSubstitution -> argMapping.type.staticApply(typeArgs) ?: return null
                       is ApplicationParameter.StaticTypeSubstitution -> argMapping.type
                   }
                }

                val appliedSupers = superTypes.map { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> superType
                        is SuperType.DynamicSuper -> SuperType.StaticSuper(superType.type.staticApply(typeArgs) ?: return null)
                    }
                }

                return NonGenericType.StaticAppliedType(
                    baseType = baseType,
                    typeArgs = mappedArgs,
                    superTypes = appliedSupers
                )
            }

            override fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType? {
                val mappedArgs: List<ApplicationParameter> = typeArgMapping.map { argMapping ->
                    when (argMapping) {
                        is ApplicationParameter.ParamSubstitution -> typeArgs.getOrNull(argMapping.param) ?: return null
                        is ApplicationParameter.DynamicTypeSubstitution -> {
                            ApplicationParameter.DynamicTypeSubstitution(argMapping.type.dynamicApply(typeArgs) ?: return null)
                        }
                        is ApplicationParameter.StaticTypeSubstitution -> argMapping
                    }
                }

                val appliedSupers = superTypes.map { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> superType
                        is SuperType.DynamicSuper -> SuperType.DynamicSuper(superType.type.dynamicApply(typeArgs) ?: return null)
                    }
                }

                return DynamicAppliedType(
                    baseType = baseType,
                    typeArgMapping = mappedArgs,
                    superTypes = appliedSupers
                )
            }

        }

        abstract fun staticApply(typeArgs: List<NonGenericType>): NonGenericType.StaticAppliedType?

        abstract fun dynamicApply(typeArgs: List<ApplicationParameter>): DynamicAppliedType?

        fun apply(typeArgs: List<ApplicationParameter>): Type? {
            val nonGenericArgs: List<ApplicationParameter.StaticTypeSubstitution>? = typeArgs.castIfAllInstance()
            
            return if (nonGenericArgs == null) {
                dynamicApply(typeArgs)
            } else {
                staticApply(nonGenericArgs.map { it.type })
            }
        }

        fun staticApply(vararg typeArgs: NonGenericType): NonGenericType.StaticAppliedType? = staticApply(typeArgs.toList())

        fun forceStaticApply(typeArgs: List<NonGenericType>): NonGenericType.StaticAppliedType
                = staticApply(typeArgs) ?: throw TypeApplicationException("Failed to static apply type args $typeArgs to $info")

        fun forceStaticApply(vararg typeArgs: NonGenericType): NonGenericType.StaticAppliedType
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

fun directType(fullName: String, vararg superType: Type.NonGenericType)
        = Type.NonGenericType.DirectType(info(fullName), superType.map { SuperType.StaticSuper(it) })

fun typeTemplate(fullName: String, typeParams: List<String>, superTypes: List<Type>) = Type.GenericType.TypeTemplate(
    info(fullName), typeParams, superTypes.map {
        when (it) {
            is Type.NonGenericType -> SuperType.StaticSuper(it)
            is Type.GenericType -> SuperType.DynamicSuper(it)
        }
    }
)
