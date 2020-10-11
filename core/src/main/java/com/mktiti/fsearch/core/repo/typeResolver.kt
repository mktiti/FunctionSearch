package com.mktiti.fsearch.core.repo

import com.mktiti.fsearch.core.type.*
import com.mktiti.fsearch.core.type.Type.NonGenericType
import com.mktiti.fsearch.core.type.Type.NonGenericType.DirectType
import com.mktiti.fsearch.core.util.liftNull

interface TypeResolver {

    operator fun get(info: MinimalInfo): DirectType?

    fun template(info: MinimalInfo): TypeTemplate?

    operator fun get(info: CompleteMinInfo.Static): NonGenericType? {
        return if (info.args.isEmpty()) {
            get(info.base)
        } else {
            val template = template(info.base) ?: return null
            val args = info.args.map(this::get).liftNull() ?: return null

            template.staticApply(args)
        }
    }

    operator fun get(info: CompleteMinInfo.Dynamic): Type.DynamicAppliedType? {
        val template = template(info.base) ?: return null
        return template.dynamicApply(info.args)
    }

    fun getAny(info: CompleteMinInfo<*>): SemiType? = when (info) {
        is CompleteMinInfo.Static -> get(info)
        is CompleteMinInfo.Dynamic -> get(info)
    }

    fun anyNgSuper(info: CompleteMinInfo.Static, predicate: (NonGenericType) -> Boolean): Boolean {
        val resolved = this[info] ?: return false
        return if (predicate(resolved)) {
            true
        } else {
            resolved.superTypes.asSequence().any { anyNgSuper(it, predicate) }
        }
    }

}

class SingleRepoTypeResolver(
        private val repo: TypeRepo
) : TypeResolver {

    override fun get(info: MinimalInfo) = repo[info]

    override fun template(info: MinimalInfo) = repo.template(info)

}

class SimpleMultiRepoTypeResolver(
        private val repos: Collection<TypeRepo>
) : TypeResolver {

    private fun <T : Any> first(query: (TypeRepo) -> T?): T? = repos.asSequence().mapNotNull(query).firstOrNull()

    override fun get(info: MinimalInfo) = first { it[info] }

    override fun template(info: MinimalInfo) = first { it.template(info) }

}
