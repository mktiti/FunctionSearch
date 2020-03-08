sealed class ApplicationParameter {

    class ParamSubstitution(val param: String) : ApplicationParameter()

    class DynamicTypeSubstitution(val type: Type.GenericType.DynamicAppliedType) : ApplicationParameter()

    class StaticTypeSubstitution(val type: Type.NonGenericType) : ApplicationParameter()

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

    abstract val name: String
    abstract val superTypes: List<SuperType>

    abstract val typeParamString: String
    val fullName by lazy {
        buildString {
            append(name)
            append(typeParamString)
        }
    }

    val supersTree: TreeNode<Type> by lazy {
        TreeNode(this, superTypes.map { it.type.supersTree })
    }

    sealed class NonGenericType(
        override val name: String,
        override val superTypes: List<SuperType.StaticSuper>
    ) : Type() {

        class DirectType(
            name: String,
            superTypes: List<SuperType.StaticSuper>
        ) : NonGenericType(name, superTypes) {

            override val typeParamString: String
                get() = ""

        }

        class StaticAppliedType(
            val baseType: GenericType.TypeTemplate,
            val typeArgs: Map<String, NonGenericType>,
            superTypes: List<SuperType.StaticSuper>
        ) : NonGenericType(baseType.name, superTypes) {

            override val typeParamString = baseType.typeParams.genericString {
                val arg = typeArgs[it]?.fullName ?: throw TypeException("Non fully applied SAT (missing ${baseType.fullName}->$it)")
                "$it = $arg"
            }

        }

    }

    sealed class GenericType(
        override val superTypes: List<SuperType>
    ) : Type() {

        class TypeTemplate(
            override val name: String,
            val typeParams: List<String>,
            superTypes: List<SuperType>
        ) : GenericType(superTypes) {

            override val typeParamString: String
                get() = typeParams.genericString { it }

            override fun staticApply(typeArgs: Map<String, NonGenericType>): NonGenericType.StaticAppliedType? {
                if (!typeArgs.keys.containsAll(typeParams)) {
                    return null
                }

                val appliedSupers = superTypes.map { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> superType
                        is SuperType.DynamicSuper -> SuperType.StaticSuper(superType.type.staticApply(typeArgs) ?: return null)
                    }
                }

                return NonGenericType.StaticAppliedType(
                    baseType = this,
                    typeArgs = typeArgs,
                    superTypes = appliedSupers
                )
            }

            override fun dynamicApply(typeArgs: Map<String, ApplicationParameter>): DynamicAppliedType? {
                if (!typeArgs.keys.containsAll(typeParams)) {
                    return null
                }

                val appliedSupers = superTypes.map { superType ->
                    when (superType) {
                        is SuperType.StaticSuper -> superType
                        is SuperType.DynamicSuper -> SuperType.DynamicSuper(superType.type.dynamicApply(typeArgs) ?: return null)
                    }
                }

                return DynamicAppliedType(
                    baseType = this,
                    typeArgMap = typeArgs,
                    superTypes = appliedSupers
                )
            }

        }

        class DynamicAppliedType(
            val baseType: TypeTemplate,
            val typeArgMap: Map<String, ApplicationParameter>,
            superTypes: List<SuperType>
        ) : GenericType(superTypes) {

            override val name: String
                get() = baseType.name

            override val typeParamString = baseType.typeParams.genericString { typeParam ->
                "$typeParam = " + when (val arg = typeArgMap[typeParam]) {
                    is ApplicationParameter.ParamSubstitution -> arg.param
                    is ApplicationParameter.DynamicTypeSubstitution -> arg.type.fullName
                    is ApplicationParameter.StaticTypeSubstitution -> arg.type.fullName
                    else -> throw TypeException("Unmatched type argument $typeParam in ${baseType.fullName}")
                }
            }

            override fun staticApply(typeArgs: Map<String, NonGenericType>): NonGenericType.StaticAppliedType? {
                val staticArgs = typeArgMap.mapValues { (_, dynArg) ->
                    when (dynArg) {
                        is ApplicationParameter.ParamSubstitution -> typeArgs[dynArg.param] ?: return null
                        is ApplicationParameter.DynamicTypeSubstitution -> dynArg.type.staticApply(typeArgs) ?: return null
                        is ApplicationParameter.StaticTypeSubstitution -> dynArg.type
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
                    typeArgs = staticArgs,
                    superTypes = appliedSupers
                )
            }

            override fun dynamicApply(typeArgs: Map<String, ApplicationParameter>): DynamicAppliedType? {
                val appliedArgMap = typeArgMap.mapValues { (_, dynArg) ->
                    when (dynArg) {
                        is ApplicationParameter.ParamSubstitution -> typeArgs[dynArg.param] ?: return null
                        is ApplicationParameter.StaticTypeSubstitution -> dynArg
                        is ApplicationParameter.DynamicTypeSubstitution -> {
                            ApplicationParameter.DynamicTypeSubstitution(dynArg.type.dynamicApply(typeArgs) ?: return null)
                        }
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
                    typeArgMap = appliedArgMap,
                    superTypes = appliedSupers
                )
            }

        }

        abstract fun staticApply(typeArgs: Map<String, NonGenericType>): NonGenericType.StaticAppliedType?

        abstract fun dynamicApply(typeArgs: Map<String, ApplicationParameter>): DynamicAppliedType?

        fun apply(typeArgs: Map<String, ApplicationParameter>): Type? {
            val nonGenericArgs: Map<String, ApplicationParameter.StaticTypeSubstitution>? = typeArgs.castIfAllValuesInstance()
            
            return if (nonGenericArgs == null) {
                dynamicApply(typeArgs)
            } else {
                staticApply(nonGenericArgs.mapValues { it.value.type })
            }
        }

        fun staticApply(vararg typeArgs: Pair<String, NonGenericType>): NonGenericType.StaticAppliedType?
                = staticApply(typeArgs.toMap())

        fun forceStaticApply(typeArgs: Map<String, NonGenericType>): NonGenericType.StaticAppliedType
                = staticApply(typeArgs) ?: throw TypeApplicationException("Failed to static apply type args to $name")

        fun forceStaticApply(vararg typeArgs: Pair<String, NonGenericType>): NonGenericType.StaticAppliedType
                = forceStaticApply(typeArgs.toMap())

        fun dynamicApply(vararg typeArgs: Pair<String, ApplicationParameter>): DynamicAppliedType?
                = dynamicApply(typeArgs.toMap())

        fun forceDynamicApply(typeArgs: Map<String, ApplicationParameter>): DynamicAppliedType
                = dynamicApply(typeArgs) ?: throw TypeApplicationException("Failed to dynamically apply type args to $name")

        fun forceDynamicApply(vararg typeArgs: Pair<String, ApplicationParameter>): DynamicAppliedType
                = forceDynamicApply(typeArgs.toMap())

        fun forceApply(typeArgs: Map<String, ApplicationParameter>): Type
                = apply(typeArgs) ?: throw TypeApplicationException("Failed to apply type args to $name")

    }

}

fun directType(name: String, vararg superType: Type.NonGenericType)
        = Type.NonGenericType.DirectType(name, superType.map { SuperType.StaticSuper(it) })

fun typeTemplate(name: String, typeParams: List<String>, superType: List<Type>) = Type.GenericType.TypeTemplate(
    name, typeParams, superType.map {
        when (it) {
            is Type.NonGenericType -> SuperType.StaticSuper(it)
            is Type.GenericType -> SuperType.DynamicSuper(it)
        }
    }
)
