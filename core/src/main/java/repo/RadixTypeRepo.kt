package repo

import PrefixTree
import TypeTemplate
import Type.NonGenericType.*
import TypeBounds
import ApplicationParameter.Substitution.TypeSubstitution.*
import Type
import TypeInfo
import type.MinimalInfo
import type.get

class RadixTypeRepo(
        private val javaRepo: JavaRepo,
        private val directs: PrefixTree<String, DirectType>,
        private val templates: PrefixTree<String, TypeTemplate>
) : TypeRepo {

    override val rootType: DirectType
        get() = javaRepo.objectType

    override val defaultTypeBounds = TypeBounds(
            upperBounds = setOf(StaticTypeSubstitution(rootType))
    )

    private val rootSuper = listOf(SuperType.StaticSuper.EagerStatic(rootType))
    private val funTypeInfo = TypeInfo("TestFun", emptyList(), "")
    private val funTypeCache: MutableMap<Int, TypeTemplate> = HashMap()

    private fun createFunType(paramCount: Int): TypeTemplate {
        return TypeTemplate(
                info = funTypeInfo.copy(name = funTypeInfo.name + paramCount),
                typeParams = (0..paramCount).map { typeParam(('A' + it).toString()) },
                superTypes = rootSuper
        )
    }

    // TODO optimize or remove
    override val allTypes: Collection<Type>
        get() = directs.toList()

    // TODO optimize or remove
    override val allTemplates: Collection<TypeTemplate>
        get() = templates.toList()

    // TODO remove?
    override fun get(name: String) = directs[name.split(".")]

    override fun get(info: MinimalInfo): DirectType? = directs[info]

    // TODO remove?
    override fun template(name: String): TypeTemplate? = templates[name.split(".")]

    override fun template(info: MinimalInfo): TypeTemplate? = templates[info]

    override fun functionType(paramCount: Int): TypeTemplate = funTypeCache.computeIfAbsent(paramCount, this::createFunType)

}
