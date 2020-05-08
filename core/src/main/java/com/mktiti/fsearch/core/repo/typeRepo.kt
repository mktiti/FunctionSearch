package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.ApplicationParameter.Substitution.TypeSubstitution.StaticTypeSubstitution
import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SuperType.DynamicSuper.EagerDynamic
import com.mktiti.fsearch.core.type.SuperType.StaticSuper
import com.mktiti.fsearch.core.type.SuperType.StaticSuper.EagerStatic
import com.mktiti.fsearch.core.type.Type
import com.mktiti.fsearch.core.type.Type.DynamicAppliedType
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeBounds
import com.mktiti.fsearch.core.type.TypeInfo
import com.mktiti.fsearch.core.type.TypeParameter
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.type.info
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.setOf

interface TypeRepo {

    val rootType: DirectType
    val defaultTypeBounds: TypeBounds

    val allTypes: Collection<Type>
    val allTemplates: Collection<TypeTemplate>

    operator fun get(name: String, allowSimple: Boolean = false): DirectType?

    operator fun get(info: MinimalInfo): DirectType?

    fun template(name: String, allowSimple: Boolean = false): TypeTemplate?

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

    fun createDirect(fullName: String, vararg superTypes: NonGenericType): DirectType {
        return DirectType(info(fullName), superTypes.map { EagerStatic(it) }).also {
            this += it
        }
    }

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

    override fun get(name: String, allowSimple: Boolean): DirectType? = types[name] ?: if (allowSimple && !name.contains(".")) {
        types.values.find { it.info.name == name }
    } else {
        null
    }

    override fun get(info: MinimalInfo): DirectType? = get(info.toString())

    override fun template(name: String, allowSimple: Boolean): TypeTemplate? = templates[name] ?: if (allowSimple && !name.contains(".")) {
        templates.values.find { it.info.name == name }
    } else {
        null
    }

    override fun template(info: MinimalInfo): TypeTemplate? = template(info.toString())

    override fun functionType(paramCount: Int): TypeTemplate = funTypeCache.computeIfAbsent(paramCount, this::createFunType)

    override fun plusAssign(type: DirectType) {
        types[type.info.fullName] = type
    }

    override fun plusAssign(template: TypeTemplate) {
        templates[template.info.fullName] = template
    }

}
