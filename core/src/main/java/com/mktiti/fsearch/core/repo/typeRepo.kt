package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import kotlin.collections.set

interface TypeRepo {

    val allTypes: Collection<DirectType>
    val allTemplates: Collection<TypeTemplate>

    operator fun get(name: String, allowSimple: Boolean = false): DirectType?

    operator fun get(info: MinimalInfo): DirectType?

    fun semi(info: MinimalInfo): SemiType? = get(info) ?: template(info)

    fun template(name: String, allowSimple: Boolean = false): TypeTemplate?

    fun template(info: MinimalInfo): TypeTemplate?

    fun typeParam(sign: String, bounds: TypeBounds): TypeParameter = TypeParameter(sign, bounds)

}

interface MutableTypeRepo : TypeRepo {

    operator fun plusAssign(type: DirectType)

    operator fun plusAssign(template: TypeTemplate)

    fun createDirect(fullName: String, superTypes: List<TypeHolder.Static>, samType: SamType.DirectSam? = null): DirectType {
        return DirectType(info(fullName).minimal, superTypes = superTypes, samType = samType, virtual = false).also {
            this += it
        }
    }

    fun createDirect(fullName: String, vararg superTypes: NonGenericType, samType: SamType.DirectSam? = null): DirectType {
        return createDirect(fullName, TypeHolder.staticDirects(superTypes.toList()), samType)
    }

    fun createTemplate(fullName: String, typeParams: List<TypeParameter>, superTypes: List<Type<*>>, samType: SamType.GenericSam? = null): TypeTemplate {
        return TypeTemplate(
                info = info(fullName).minimal,
                typeParams = typeParams,
                superTypes = TypeHolder.anyDirects(superTypes),
                samType = samType
        ).also {
            this += it
        }
    }

}

class SetTypeRepo : MutableTypeRepo {

    private val types: MutableMap<String, DirectType> = HashMap()
    private val templates: MutableMap<String, TypeTemplate> = HashMap()

    override val allTypes: Collection<DirectType>
        get() = types.map { it.value }

    override val allTemplates: Collection<TypeTemplate>
        get() = templates.map { it.value }

    override fun get(name: String, allowSimple: Boolean): DirectType? = types[name] ?: if (allowSimple && !name.contains(".")) {
        types.values.find { it.info.simpleName == name }
    } else {
        null
    }

    override fun get(info: MinimalInfo): DirectType? = get(info.toString())

    override fun template(name: String, allowSimple: Boolean): TypeTemplate? = templates[name] ?: if (allowSimple && !name.contains(".")) {
        templates.values.find { it.info.simpleName == name }
    } else {
        null
    }

    override fun template(info: MinimalInfo): TypeTemplate? = template(info.toString())

    override fun plusAssign(type: DirectType) {
        types[type.info.fullName] = type
    }

    override fun plusAssign(template: TypeTemplate) {
        templates[template.info.fullName] = template
    }

}
