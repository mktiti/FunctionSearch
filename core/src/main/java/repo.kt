import ApplicationParameter.TypeSubstitution.StaticTypeSubstitution
import SuperType.DynamicSuper
import SuperType.StaticSuper
import Type.DynamicAppliedType
import Type.NonGenericType
import Type.NonGenericType.DirectType

interface TypeRepo {

    val rootType: DirectType
    val defaultTypeBounds: TypeBounds

    operator fun get(name: String): DirectType?

    fun template(name: String): TypeTemplate?

    fun functionType(paramCount: Int): TypeTemplate

    fun typeParam(sign: String, bounds: TypeBounds = defaultTypeBounds): TypeParameter = TypeParameter(sign, bounds)

}

interface MutableTypeRepo : TypeRepo {

    operator fun plusAssign(type: DirectType)

    operator fun plusAssign(template: TypeTemplate)

    // TODO
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

    // TODO
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
        lowerBounds = emptySet(),
        upperBounds = setOf(StaticTypeSubstitution(rootType))
    )

    private val types: MutableMap<String, DirectType> = HashMap()
    private val templates: MutableMap<String, TypeTemplate> = HashMap()

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
