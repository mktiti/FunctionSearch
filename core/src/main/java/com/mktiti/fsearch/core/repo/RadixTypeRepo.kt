package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.MinimalInfo
import com.mktiti.fsearch.core.type.SemiType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.type.TypeTemplate
import com.mktiti.fsearch.core.type.get
import com.mktiti.fsearch.util.PrefixTree

private typealias SimpleMap<T> = Map<String, List<T>>
private typealias Store<T> = PrefixTree<String, T>

class RadixTypeRepo(
        private val directs: Store<DirectType>,
        private val templates: Store<TypeTemplate>
) : TypeRepo {

    // TODO think through - maybe cache?
    private fun <T : SemiType> simpleNames(data: Store<T>): SimpleMap<T>
            = data.groupBy { it.info.simpleName }

    private val directSimpleIndex = simpleNames(directs)
    private val templateSimpleIndex = simpleNames(templates)

    // TODO optimize or remove
    override val allTypes: Collection<DirectType>
        get() = directs.toList()

    // TODO optimize or remove
    override val allTemplates: Collection<TypeTemplate>
        get() = templates.toList()

    private fun <T : SemiType> find(
            name: String,
            allowSimple: Boolean,
            store: Store<T>,
            simpleMap: SimpleMap<T>,
            simpleSelect: (Collection<T>) -> T?
    ): T? {
        val nameParts = name.split(".")
        return when (val direct = store[nameParts]) {
            null -> {
                if (allowSimple && nameParts.all { it.firstOrNull()?.isUpperCase() == true }) {
                    simpleMap[name]?.let(simpleSelect)
                } else {
                    null
                }
            }
            else -> direct
        }
    }

    // TODO remove?
    override fun get(name: String, allowSimple: Boolean) = find(name, allowSimple, directs, directSimpleIndex) {
        it.firstOrNull()
    }

    override fun get(info: MinimalInfo): DirectType? = directs[info]

    // TODO remove?
    override fun template(
            name: String,
            allowSimple: Boolean,
            paramCount: Int?
    ): TypeTemplate? {
        val simplePred: (Collection<TypeTemplate>) -> TypeTemplate? = if (paramCount == null) {
            Collection<TypeTemplate>::firstOrNull
        } else {
            { it.find { t -> t.typeParamCount == paramCount } }
        }

        return find(name, allowSimple, templates, templateSimpleIndex, simplePred)
    }

    override fun template(info: MinimalInfo): TypeTemplate? = templates[info]

}
