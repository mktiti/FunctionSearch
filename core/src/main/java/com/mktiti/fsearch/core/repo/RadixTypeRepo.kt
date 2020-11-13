package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.elseNull
import com.mktiti.fsearch.util.PrefixTree

class RadixTypeRepo(
        private val javaRepo: JavaRepo,
        private val directs: PrefixTree<String, DirectType>,
        private val templates: PrefixTree<String, TypeTemplate>
) : TypeRepo {

    // TODO think through - maybe cache?
    private fun <T : SemiType> simpleNames(data: PrefixTree<String, T>) = data.map { it.info.simpleName to it }.toMap()

    private val directSimpleIndex = simpleNames(directs)
    private val templateSimpleIndex = simpleNames(templates)

    // TODO optimize or remove
    override val allTypes: Collection<DirectType>
        get() = directs.toList()

    // TODO optimize or remove
    override val allTemplates: Collection<TypeTemplate>
        get() = templates.toList()

    private fun <T : Any> bySimple(name: String, allowSimple: Boolean, find: (String) -> T?): T?
            = (allowSimple && !name.contains(".")).elseNull { find(name) }

    // TODO remove?
    override fun get(name: String, allowSimple: Boolean)
            = directs[name.split(".")] ?: bySimple(name, allowSimple, directSimpleIndex::get)

    override fun get(info: MinimalInfo): DirectType? = directs[info]

    // TODO remove?
    override fun template(name: String, allowSimple: Boolean): TypeTemplate?
            = templates[name.split(".")] ?: bySimple(name, allowSimple, templateSimpleIndex::get)

    override fun template(info: MinimalInfo): TypeTemplate? = templates[info]

}
