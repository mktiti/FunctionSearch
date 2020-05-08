package repo

import ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import SuperType
import SuperType.*
import SuperType.StaticSuper.*
import SuperType.DynamicSuper.*
import Type
import Type.DynamicAppliedType
import Type.NonGenericType
import Type.NonGenericType.DirectType
import TypeBounds
import TypeInfo
import TypeParameter
import TypeTemplate
import info
import type.MinimalInfo

interface TypeRepo {

    val rootType: DirectType
    val defaultTypeBounds: TypeBounds

    val allTypes: Collection<Type>
    val allTemplates: Collection<TypeTemplate>

    operator fun get(name: String): DirectType?

    operator fun get(info: MinimalInfo): DirectType?

    fun template(name: String): TypeTemplate?

    fun template(info: MinimalInfo): TypeTemplate?

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
                EagerStatic(creator(self))
            }
        }
    }

    /*
    fun createSelfRefTemplate(fullName: String, typeParams: List<TypeParameter>, superCreators: List<(self: TypeTemplate) -> Type>): TypeTemplate {
        val supers: MutableList<SuperType<Type>> = ArrayList(superCreators.size)

        return TypeTemplate(
                info = info(fullName),
                typeParams = typeParams,
                superTypes = supers
        ).also { self ->
            this += self

            supers += superCreators.map { creator ->
                when (val created = creator(self)) {
                    is NonGenericType -> EagerStatic(created)
                    is DynamicAppliedType -> EagerDynamic(created)
                }
            }
        }
    }
     */

    // TODO - save referenced properly or not at all
    fun createDirect(fullName: String, vararg superTypes: NonGenericType): DirectType {
        return DirectType(info(fullName), superTypes.map { EagerStatic(it) }).also {
            this += it
        }
        /*
        superTypes.forEach { superType ->
            when (superType) {
                is DirectType -> this += superType
                is NonGenericType.StaticAppliedType -> this += superType.baseType
            }
        }
        return DirectType(info(fullName), superTypes.map { EagerStatic(it) }).also {
            this += it
        }
         */
    }

    // TODO - save referenced properly or not at all
    fun createTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type>): TypeTemplate {
        return TypeTemplate(
                info = info(fullName),
                typeParams = typeParams,
                superTypes = superTypes.map {
                    when (it) {
                        is NonGenericType -> EagerStatic(it)
                        is DynamicAppliedType -> EagerDynamic(it)
                    }
                }
        ).also {
            this += it
        }
        /*superTypes.forEach { superType ->
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
                        is NonGenericType -> EagerStatic(it)
                        is DynamicAppliedType -> EagerDynamic(it)
                    }
                }
        ).also {
            this += it
        }
         */
    }

}

class SetTypeRepo(
        rootInfo: TypeInfo,
        private val funTypeInfo: TypeInfo
) : MutableTypeRepo {

    override val rootType = DirectType(rootInfo, emptyList())
    override val defaultTypeBounds = TypeBounds(
            upperBounds = setOf(StaticTypeSubstitution(rootType))
    )

    private val types: MutableMap<String, DirectType> = HashMap()
    private val templates: MutableMap<String, TypeTemplate> = HashMap()

    override val allTypes: Collection<Type>
        get() = types.map { it.value }

    override val allTemplates: Collection<TypeTemplate>
        get() = templates.map { it.value }

    private val rootSuper = listOf(EagerStatic(rootType))
    private val funTypeCache: MutableMap<Int, TypeTemplate> = HashMap()

    init {
        this += rootType
    }

    private fun createFunType(paramCount: Int): TypeTemplate {
        return TypeTemplate(
                info = funTypeInfo.copy(name = funTypeInfo.name + paramCount),
                typeParams = (0..paramCount).map { typeParam(('A' + it).toString()) },
                superTypes = rootSuper
        )
    }

    override fun get(name: String): DirectType? = types[name]

    override fun get(info: MinimalInfo): DirectType? = get(info.toString())

    override fun template(name: String): TypeTemplate? = templates[name]

    override fun template(info: MinimalInfo): TypeTemplate? = template(info.toString())

    override fun functionType(paramCount: Int): TypeTemplate = funTypeCache.computeIfAbsent(paramCount, this::createFunType)

    override fun plusAssign(type: DirectType) {
        types[type.info.fullName] = type
    }

    override fun plusAssign(template: TypeTemplate) {
        templates[template.info.fullName] = template
    }

}
