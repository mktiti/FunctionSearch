import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import SuperType.DynamicSuper
import SuperType.StaticSuper
import Type.DynamicAppliedType
import Type.NonGenericType
import Type.NonGenericType.DirectType

interface TypeRepo {

    val rootType: DirectType
    val defaultTypeBounds: TypeBounds

    val allTypes: Collection<Type>
    val allTemplates: Collection<TypeTemplate>

    operator fun get(name: String): DirectType?

    fun template(name: String): TypeTemplate?

    fun functionType(paramCount: Int): TypeTemplate

    fun typeParam(sign: String, bounds: TypeBounds = defaultTypeBounds): TypeParameter = TypeParameter(sign, bounds)

}

interface MutableTypeRepo : TypeRepo {

    operator fun plusAssign(type: DirectType)

    operator fun plusAssign(template: TypeTemplate)

    fun createSelfRefDirect(fullName: String, superCreators: List<(self: DirectType) -> NonGenericType>): DirectType {
        val supers: MutableList<StaticSuper> = ArrayList(superCreators.size)

        return DirectType(info(fullName), supers).also { self ->
            this += self

            supers += superCreators.map { creator ->
                StaticSuper(creator(self))
            }
        }
    }

    fun createSelfRefTemplate(fullName: String, typeParams: List<TypeParameter>, superCreators: List<(self: TypeTemplate) -> Type>): TypeTemplate {
        val supers: MutableList<SuperType> = ArrayList(superCreators.size)

        return TypeTemplate(
            info = info(fullName),
            typeParams = typeParams,
            superTypes = supers
        ).also { self ->
            this += self

            supers += superCreators.map { creator ->
                when (val created = creator(self)) {
                    is NonGenericType -> StaticSuper(created)
                    is DynamicAppliedType -> DynamicSuper(created)
                }
            }
        }
    }

    // TODO - save referenced properly or not at all
    fun createDirect(fullName: String, vararg superTypes: NonGenericType): DirectType {
        superTypes.forEach { superType ->
            when (superType) {
                is DirectType -> this += superType
                is NonGenericType.StaticAppliedType -> this += superType.baseType
            }
        }
        return DirectType(info(fullName), superTypes.map { StaticSuper(it) }).also {
            this += it
        }
    }

    // TODO - save referenced properly or not at all
    fun createTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type>): TypeTemplate {
        superTypes.forEach { superType ->
            when (superType) {
                is DirectType -> this += superType
                is NonGenericType.StaticAppliedType -> this += superType.baseType
            }
        }
        return TypeTemplate(
            info = info(fullName),
            typeParams = typeParams,
            superTypes = superTypes.map {
                when (it) {
                    is NonGenericType -> StaticSuper(it)
                    is DynamicAppliedType -> DynamicSuper(it)
                }
            }
        ).also {
            this += it
        }
    }

}

class SetTypeRepo(
    rootInfo: TypeInfo,
    private val funTypeInfo: TypeInfo
) : MutableTypeRepo {

    override val rootType = DirectType(rootInfo, emptyList())
    override val defaultTypeBounds = TypeBounds(
        //lowerBounds = emptySet(),
        upperBounds = setOf(StaticTypeSubstitution(rootType))
    )

    private val types: MutableMap<String, DirectType> = HashMap()
    private val templates: MutableMap<String, TypeTemplate> = HashMap()

    override val allTypes: Collection<Type>
        get() = types.map { it.value }

    override val allTemplates: Collection<TypeTemplate>
        get() = templates.map { it.value }

    private val rootSuper = listOf(StaticSuper(rootType))
    private val funTypeCache: MutableMap<Int, TypeTemplate> = HashMap()

    init {
        this += rootType
    }

    private fun createFunType(paramCount: Int): TypeTemplate {
        return TypeTemplate(
            info = funTypeInfo.copy(name = funTypeInfo.name + paramCount),
            typeParams = (0 .. paramCount).map { typeParam(('A' + it).toString()) },
            superTypes = rootSuper
        )
    }

    override fun get(name: String): DirectType? = types[name]

    override fun template(name: String): TypeTemplate? = templates[name]

    override fun functionType(paramCount: Int): TypeTemplate = funTypeCache.computeIfAbsent(paramCount, this::createFunType)

    override fun plusAssign(type: DirectType) {
        types[type.info.fullName] = type
    }

    override fun plusAssign(template: TypeTemplate) {
        templates[template.info.fullName] = template
    }

}
